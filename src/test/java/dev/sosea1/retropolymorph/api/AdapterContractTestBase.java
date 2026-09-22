package dev.sosea1.retropolymorph.api;

import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reusable contract test harness verifying standard adapter and context invariants.
 */
public abstract class AdapterContractTestBase<C extends Container> {

    @BeforeAll
    public static void initBootstrap() {
        Bootstrap.register();
    }

    /** Creates the adapter under test. */
    protected abstract RecipeSelectionAdapter createAdapter();

    /** Creates a container instance that should match the adapter. */
    protected abstract C createMatchingContainer();

    /** Creates a container instance that is unrelated to the adapter. */
    protected abstract Container createUnrelatedContainer();

    /** Populates the container with inputs that yield valid recipes if applicable. */
    protected void populateValidInputs(C container) {
    }

    /** Returns a world reference for recipe discovery (null if stubs allow null). */
    protected World getWorld() {
        return null;
    }

    @Test
    public void unrelatedContainerReturnsMiss() {
        RecipeSelectionAdapter adapter = createAdapter();
        Container unrelated = createUnrelatedContainer();

        AdapterDetectionResult result = adapter.probe(unrelated);
        assertNotNull(result);
        assertFalse(result.isMatch(), "Unrelated container must not match");
    }

    @Test
    public void nullContainerHandledSafely() {
        RecipeSelectionAdapter adapter = createAdapter();

        AdapterDetectionResult result = adapter.probe(null);
        assertNotNull(result);
        assertFalse(result.isMatch(), "Null container must not match");
    }

    @Test
    public void matchingContainerProducesValidContext() {
        RecipeSelectionAdapter adapter = createAdapter();
        C container = createMatchingContainer();

        AdapterDetectionResult result = adapter.probe(container);
        assertNotNull(result);
        assertTrue(result.isMatch(), "Expected adapter to match valid container");

        SelectionContext context = result.getContext();
        assertNotNull(context, "Matched result must supply a SelectionContext");
        assertEquals(container, context.getContainer(), "Context must reference the original container");
    }

    @Test
    public void contextReportsValidMetadata() {
        RecipeSelectionAdapter adapter = createAdapter();
        C container = createMatchingContainer();

        AdapterDetectionResult result = adapter.probe(container);
        assertTrue(result.isMatch());
        SelectionContext context = result.getContext();
        assertNotNull(context);

        assertNotNull(context.getPersistencePolicy(), "Persistence policy must not be null");
        assertNotNull(context.getSelectorPlacement(), "Selector placement must not be null");
        assertNotNull(context.getSelectionScope(), "Selection scope must not be null");
    }

    @Test
    public void optionKeysAreWireSafeAndDistinct() {
        RecipeSelectionAdapter adapter = createAdapter();
        C container = createMatchingContainer();
        populateValidInputs(container);

        SelectionContext context = adapter.probe(container).getContext();
        if (context == null) {
            return;
        }

        List<RecipeOption> options = context.findOptions(getWorld());
        assertNotNull(options, "findOptions must return a non-null list");

        Set<String> keys = new HashSet<String>();
        for (RecipeOption option : options) {
            assertNotNull(option);
            String key = option.getRecipeKey();
            assertNotNull(key, "Recipe option key must not be null");
            assertTrue(RecipeKey.isWireSafe(key), "Recipe key must be wire-safe: " + key);
            assertTrue(keys.add(key), "Recipe option keys must be unique: " + key);
            assertNotNull(option.getOutput(), "Recipe option output stack must not be null");
        }
    }

    @Test
    public void selectionWorkflowInvariants() {
        RecipeSelectionAdapter adapter = createAdapter();
        C container = createMatchingContainer();
        populateValidInputs(container);

        SelectionContext context = adapter.probe(container).getContext();
        if (context == null) {
            return;
        }

        List<RecipeOption> options = context.findOptions(getWorld());
        assertNotNull(options);

        // Unknown key should be rejected
        boolean selectedUnknown = context.select("retropolymorph_invalid_domain:unknown_key", getWorld());
        assertFalse(selectedUnknown, "Selecting an invalid key must return false");

        // Selecting a valid key if options exist
        if (!options.isEmpty()) {
            String firstKey = options.get(0).getRecipeKey();
            boolean selected = context.select(firstKey, getWorld());
            assertTrue(selected, "Selecting an available option must succeed");
            assertEquals(firstKey, context.getSelectedRecipeKey(), "Selected key must match selection");

            // Clearing selection
            context.clearSelection();
            assertEquals(null, context.getSelectedRecipeKey(), "Clearing selection must remove active key");

            // Idempotent clear
            context.clearSelection();
            assertEquals(null, context.getSelectedRecipeKey(), "Second clearSelection must be idempotent");
        }
    }

    @Test
    public void inputSnapshotAccessDoesNotMutateInventory() {
        RecipeSelectionAdapter adapter = createAdapter();
        C container = createMatchingContainer();
        populateValidInputs(container);

        SelectionContext context = adapter.probe(container).getContext();
        if (context == null) {
            return;
        }

        int count = context.getInputCount();
        assertTrue(count >= 0, "Input count must be non-negative");

        ItemStack[] before = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            ItemStack stack = context.getInputStack(i);
            before[i] = stack != null ? stack.copy() : ItemStack.EMPTY;
        }

        // Call snapshot access methods repeatedly
        for (int iter = 0; iter < 3; iter++) {
            assertEquals(count, context.getInputCount());
            for (int i = 0; i < count; i++) {
                ItemStack stack = context.getInputStack(i);
                assertTrue(ItemStack.areItemStacksEqual(before[i], stack != null ? stack : ItemStack.EMPTY),
                        "Input stack changed unexpectedly at slot " + i);
            }
            context.getClientStateToken();
        }
    }
}
