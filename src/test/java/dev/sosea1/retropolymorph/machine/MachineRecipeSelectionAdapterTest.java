package dev.sosea1.retropolymorph.machine;

import dev.sosea1.retropolymorph.api.MachineRecipeAdapter;
import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MachineRecipeSelectionAdapterTest {

    @BeforeAll
    public static void setup() {
        Bootstrap.register();
    }

    @Test
    public void cosmeticOnlySurfaceBlocksFallbackButDoesNotCreateSelector() {
        DummyContainer container = new DummyContainer();
        MachineRecipeSelectionAdapter adapter = new MachineRecipeSelectionAdapter(
                new FixedAdapter(container, new DummySurface(container, false)));

        dev.sosea1.retropolymorph.api.AdapterDetectionResult result = adapter.probe(container);
        assertTrue(result.isBlockFallback());
        assertFalse(result.isMatch());
        assertNull(result.getContext());
    }

    @Test
    public void virtualAnchorAndOwnerPersistenceAreBridged() {
        DummyContainer container = new DummyContainer();
        DummySurface surface = new DummySurface(container, true);
        MachineRecipeSelectionAdapter adapter = new MachineRecipeSelectionAdapter(
                new FixedAdapter(container, surface));

        dev.sosea1.retropolymorph.api.AdapterDetectionResult result = adapter.probe(container);
        assertTrue(result.isMatch());
        SelectionContext context = result.getContext();
        assertNotNull(context);
        assertNull(context.getResultSlot());
        SelectorPlacement placement = context.getSelectorPlacement();
        assertTrue(placement.isVisible());
        assertEquals(SelectorPlacement.AnchorMode.RESULT_SLOT, placement.getMode());
        assertEquals(91, placement.getAnchorX());
        assertEquals(37, placement.getAnchorY());
        assertFalse(context.getPersistencePolicy().supportsPlayerPreferences());
    }

    @Test
    public void hybridOwnerPersistenceUsesAndOverridesWithPlayerPreference() {
        DummyContainer container = new DummyContainer();
        DummySurface surface = new DummySurface(
                container, true, MachineRecipePersistence.OWNER_WITH_PLAYER_PROFILE);
        MachineSelectionContext context = new MachineSelectionContext(surface);

        assertTrue(context.getPersistencePolicy().supportsPlayerPreferences());
        assertTrue(context.getPersistencePolicy().playerPreferenceOverridesCurrent());
    }

    @Test
    public void machineOptionsAreWireSafeAndDeduplicatedByStableKey() {
        DummyContainer container = new DummyContainer();
        DummySurface surface = new DummySurface(container, true);
        MachineSelectionContext context = new MachineSelectionContext(surface);

        List<RecipeOption> options = context.findOptions(null);
        assertEquals(2, options.size());
        assertEquals("example:first", options.get(0).getRecipeKey());
        assertEquals("example:second", options.get(1).getRecipeKey());
    }

    @Test
    public void probeExecutesRecognizesExactlyOnceForMatchesAndBlocks() {
        DummyContainer container = new DummyContainer();
        DummySurface validSurface = new DummySurface(container, true);
        CountingAdapter validDelegate = new CountingAdapter(container, validSurface);
        MachineRecipeSelectionAdapter validAdapter = new MachineRecipeSelectionAdapter(validDelegate);

        dev.sosea1.retropolymorph.api.AdapterDetectionResult matchResult = validAdapter.probe(container);
        assertTrue(matchResult.isMatch());
        assertNotNull(matchResult.getContext());
        assertEquals(1, validDelegate.recognizeCount, "recognizes() must be called exactly once for MATCH");

        DummySurface blockedSurface = new DummySurface(container, false);
        CountingAdapter blockDelegate = new CountingAdapter(container, blockedSurface);
        MachineRecipeSelectionAdapter blockAdapter = new MachineRecipeSelectionAdapter(blockDelegate);

        dev.sosea1.retropolymorph.api.AdapterDetectionResult blockResult = blockAdapter.probe(container);
        assertTrue(blockResult.isBlockFallback());
        assertFalse(blockResult.isMatch());
        assertEquals(1, blockDelegate.recognizeCount, "recognizes() must be called exactly once for BLOCK_FALLBACK");

        CountingAdapter missDelegate = new CountingAdapter(new DummyContainer(), validSurface);
        MachineRecipeSelectionAdapter missAdapter = new MachineRecipeSelectionAdapter(missDelegate);

        dev.sosea1.retropolymorph.api.AdapterDetectionResult missResult = missAdapter.probe(container);
        assertTrue(missResult.isMiss());
        assertEquals(1, missDelegate.recognizeCount, "recognizes() must be called exactly once for MISS");
    }

    private static final class CountingAdapter implements MachineRecipeAdapter {
        private final Container expected;
        private final MachineRecipeSurface surface;
        int recognizeCount = 0;

        private CountingAdapter(Container expected, MachineRecipeSurface surface) {
            this.expected = expected;
            this.surface = surface;
        }

        @Override
        public boolean recognizes(Container container) {
            recognizeCount++;
            return container == this.expected;
        }

        @Override
        public MachineRecipeSurface openSurface(Container container) {
            return container == this.expected ? this.surface : null;
        }
    }

    private static final class FixedAdapter implements MachineRecipeAdapter {
        private final Container expected;
        private final MachineRecipeSurface surface;

        private FixedAdapter(Container expected, MachineRecipeSurface surface) {
            this.expected = expected;
            this.surface = surface;
        }

        @Override
        public boolean recognizes(Container container) {
            return container == this.expected;
        }

        @Override
        public MachineRecipeSurface openSurface(Container container) {
            return container == this.expected ? this.surface : null;
        }
    }

    private static final class DummySurface implements MachineRecipeSurface {
        private final Container container;
        private final boolean actualOperation;
        private final MachineRecipePersistence persistence;
        private final Item item = new Item();

        private DummySurface(Container container, boolean actualOperation) {
            this(container, actualOperation, MachineRecipePersistence.OWNER);
        }

        private DummySurface(
                Container container,
                boolean actualOperation,
                MachineRecipePersistence persistence) {
            this.container = container;
            this.actualOperation = actualOperation;
            this.persistence = persistence;
        }

        @Override
        public Container getContainer() {
            return this.container;
        }

        @Override
        public Object getRecipeOwner() {
            return this;
        }

        @Override
        public int getInputCount() {
            return 1;
        }

        @Override
        public ItemStack getInputStack(int index) {
            return index == 0 ? new ItemStack(this.item) : ItemStack.EMPTY;
        }

        @Override
        public List<RecipeOption> findOptions(World world) {
            ItemStack output = new ItemStack(this.item);
            return Arrays.asList(
                    new RecipeOption("example:first", output),
                    new RecipeOption("example:first", output),
                    new RecipeOption("bad\nkey", output),
                    new RecipeOption("example:second", output));
        }

        @Override
        public boolean selectRecipe(String recipeKey, World world) {
            return true;
        }

        @Override
        public void clearRecipeSelection() {
        }

        @Override
        @Nullable
        public String getSelectedRecipeKey() {
            return null;
        }

        @Override
        public MachineRecipePersistence getPersistencePolicy() {
            return this.persistence;
        }

        @Override
        public boolean controlsActualOperation() {
            return this.actualOperation;
        }

        @Override
        @Nullable
        public Slot getResultSlot() {
            return null;
        }

        @Override
        public SelectorPlacement getSelectorPlacement() {
            return SelectorPlacement.resultSlot(91, 37, 0, 0);
        }
    }

    private static final class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    }
}
