package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.preference.ConflictFingerprint;
import dev.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SelectionServiceTest {

    private static Item testItem;
    private NBTTagCompound playerData;

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        testItem = new Item();
    }

    @BeforeEach
    public void setup() {
        this.playerData = new NBTTagCompound();
    }

    @AfterEach
    public void tearDown() {
        RecipePreferencePolicy.configure(Collections.<String>emptyList(), Collections.<String>emptyList());
        RecipePreferencePolicy.setPriority("mod:policy", 0);
        RecipePreferencePolicy.setPriority("mod:api", 0);
    }

    @Test
    public void queryReturnsAllValidDistinctRecipeOptionKeys() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:a", new ItemStack(testItem, 1)),
                new RecipeOption("mod:b", new ItemStack(testItem, 2)),
                new RecipeOption("mod:c", new ItemStack(testItem, 3)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);

        SelectionServiceResult result = SelectionService.handle(null, this.playerData, ctx, SelectionCommand.query());
        assertTrue(result.isAccepted());
        assertEquals(3, result.getOptions().size());
        assertEquals("mod:a", result.getOptions().get(0).getRecipeKey());
        assertEquals("mod:b", result.getOptions().get(1).getRecipeKey());
        assertEquals("mod:c", result.getOptions().get(2).getRecipeKey());
        assertNull(result.getSelectedRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, result.getReason());
    }

    @Test
    public void selectedKeyMustBelongToCurrentOptionSet() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:a", new ItemStack(testItem, 1)),
                new RecipeOption("mod:b", new ItemStack(testItem, 2)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);

        SelectionServiceResult validResult = SelectionService.handle(
                null, this.playerData, ctx, SelectionCommand.select("mod:a"));
        assertTrue(validResult.isAccepted());
        assertEquals("mod:a", validResult.getSelectedRecipeKey());
        assertEquals(SelectionReason.PLAYER_SELECTION, validResult.getReason());
        assertTrue(validResult.isSelectionChanged());

        // Unknown key is rejected
        SelectionServiceResult invalidResult = SelectionService.handle(
                null, this.playerData, ctx, SelectionCommand.select("mod:unknown"));
        assertFalse(invalidResult.isAccepted());
    }

    @Test
    public void unknownOrUnsafeKeyIsRejected() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:a", new ItemStack(testItem, 1)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);

        SelectionServiceResult result = SelectionService.handle(
                null, this.playerData, ctx, SelectionCommand.select("unsafe key with spaces!"));
        assertFalse(result.isAccepted());
    }

    @Test
    public void clearingSelectionRemovesExplicitSelectionAndForgetsStoredPreference() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:a", new ItemStack(testItem, 1)),
                new RecipeOption("mod:b", new ItemStack(testItem, 2)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);

        // Select mod:a and verify preference stored
        SelectionService.handle(null, this.playerData, ctx, SelectionCommand.select("mod:a"));
        String fingerprint = ConflictFingerprint.create(options);
        assertEquals("mod:a", PlayerRecipePreferences.lookup(this.playerData, fingerprint));

        // Clear selection and verify preference forgotten
        SelectionServiceResult clearResult = SelectionService.handle(
                null, this.playerData, ctx, SelectionCommand.clear());
        assertTrue(clearResult.isAccepted());
        assertNull(clearResult.getSelectedRecipeKey());
        assertNull(PlayerRecipePreferences.lookup(this.playerData, fingerprint));
        assertTrue(clearResult.isSelectionChanged());
    }

    @Test
    public void storedPlayerPreferenceWinsOverModpackDefaultPolicy() {
        RecipePreferencePolicy.setPriority("mod:policy", 50);

        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:policy", new ItemStack(testItem, 1)),
                new RecipeOption("mod:player", new ItemStack(testItem, 2)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);

        // Seed player preference for this conflict
        String fingerprint = ConflictFingerprint.create(options);
        PlayerRecipePreferences.remember(this.playerData, fingerprint, "mod:player");

        // QUERY should pick stored player preference over modpack policy
        SelectionServiceResult result = SelectionService.handle(null, this.playerData, ctx, SelectionCommand.query());
        assertEquals("mod:player", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.PLAYER_PREFERENCE, result.getReason());
    }

    @Test
    public void currentOwnerSessionSelectionRetainedWhenOverrideDisabled() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:current", new ItemStack(testItem, 1)),
                new RecipeOption("mod:preferred", new ItemStack(testItem, 2)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);
        ctx.select("mod:current", null);

        // Seed different player preference
        String fingerprint = ConflictFingerprint.create(options);
        PlayerRecipePreferences.remember(this.playerData, fingerprint, "mod:preferred");

        // With override = false, current selection is retained
        SelectionServiceResult result = SelectionService.handle(null, this.playerData, ctx, SelectionCommand.query());
        assertEquals("mod:current", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.CURRENT_CONTEXT, result.getReason());
        assertFalse(result.isSelectionChanged());
    }

    @Test
    public void contextOptingIntoOverrideReplacesCurrentSelectionFromStoredPreference() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:current", new ItemStack(testItem, 1)),
                new RecipeOption("mod:preferred", new ItemStack(testItem, 2)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT_OVERRIDE);
        ctx.select("mod:current", null);

        // Seed player preference
        String fingerprint = ConflictFingerprint.create(options);
        PlayerRecipePreferences.remember(this.playerData, fingerprint, "mod:preferred");

        // With override = true, preference replaces current
        SelectionServiceResult result = SelectionService.handle(null, this.playerData, ctx, SelectionCommand.query());
        assertEquals("mod:preferred", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.PLAYER_PREFERENCE, result.getReason());
        assertTrue(result.isSelectionChanged());
    }

    @Test
    public void staleStoredPlayerPreferenceIsRemovedWhenRecipeNotInConflict() {
        List<RecipeOption> original = Arrays.asList(
                new RecipeOption("mod:old", new ItemStack(testItem, 1)),
                new RecipeOption("mod:other", new ItemStack(testItem, 2)));
        String fingerprint = ConflictFingerprint.create(original);
        PlayerRecipePreferences.remember(this.playerData, fingerprint, "mod:old");

        // Now the conflict has changed and no longer includes mod:old, but has same fingerprint?
        // Let's test options that don't have mod:old:
        List<RecipeOption> current = Arrays.asList(
                new RecipeOption("mod:new1", new ItemStack(testItem, 1)),
                new RecipeOption("mod:new2", new ItemStack(testItem, 2)));
        MockContext ctx = new MockContext(current, SelectionPersistencePolicy.PLAYER_PERSISTENT);
        String currentFingerprint = ConflictFingerprint.create(current);
        PlayerRecipePreferences.remember(this.playerData, currentFingerprint, "mod:old");

        SelectionServiceResult result = SelectionService.handle(null, this.playerData, ctx, SelectionCommand.query());
        assertNull(PlayerRecipePreferences.lookup(this.playerData, currentFingerprint));
    }

    @Test
    public void distinctRecipeKeysWithEqualOutputsRemainDistinct() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:recipe1", new ItemStack(testItem, 1)),
                new RecipeOption("mod:recipe2", new ItemStack(testItem, 1)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);

        SelectionServiceResult result = SelectionService.handle(null, this.playerData, ctx, SelectionCommand.query());
        assertEquals(2, result.getOptions().size());
        assertEquals("mod:recipe1", result.getOptions().get(0).getRecipeKey());
        assertEquals("mod:recipe2", result.getOptions().get(1).getRecipeKey());
    }

    @Test
    public void brokenRecipesCannotCrashDiscovery() {
        SelectionContext throwingCtx = new SelectionContext() {
            @Override public Container getContainer() { return null; }
            @Override public Slot getResultSlot() { return null; }
            @Override public int getInputCount() { return 0; }
            @Override public ItemStack getInputStack(int index) { return ItemStack.EMPTY; }
            @Override public List<RecipeOption> findOptions(World world) { throw new RuntimeException("broken"); }
            @Override public boolean select(String recipeKey, World world) { return false; }
            @Override public void clearSelection() {}
            @Nullable @Override public String getSelectedRecipeKey() { return null; }
        };

        SelectionServiceResult result = SelectionService.handle(null, this.playerData, throwingCtx, SelectionCommand.query());
        assertFalse(result.isAccepted());
        assertEquals(0, result.getOptions().size());
    }

    @Test
    public void authoritativeSelectedKeyNotPresentInLiveOptionSetIsClearedBeforeSync() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:live", new ItemStack(testItem, 1)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.PLAYER_PERSISTENT);
        // Pre-select a stale key that is NOT in live options
        ctx.selectedKey = "mod:stale";

        SelectionServiceResult result = SelectionService.handle(null, this.playerData, ctx, SelectionCommand.query());
        assertNull(result.getSelectedRecipeKey());
        assertNull(ctx.selectedKey);
        assertFalse(result.isAccepted());
    }

    @Test
    public void ownerOnlyPolicyNeverStoresOrRestoresPlayerPreferences() {
        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("mod:a", new ItemStack(testItem, 1)),
                new RecipeOption("mod:b", new ItemStack(testItem, 2)));
        MockContext ctx = new MockContext(options, SelectionPersistencePolicy.OWNER_ONLY);

        SelectionServiceResult result = SelectionService.handle(
                null, this.playerData, ctx, SelectionCommand.select("mod:a"));
        assertTrue(result.isAccepted());
        assertEquals("mod:a", result.getSelectedRecipeKey());

        // Player preferences store must remain empty
        String fingerprint = ConflictFingerprint.create(options);
        assertNull(PlayerRecipePreferences.lookup(this.playerData, fingerprint));
    }

    private static final class MockContext implements SelectionContext {
        private final List<RecipeOption> options;
        private final SelectionPersistencePolicy policy;
        @Nullable String selectedKey;

        MockContext(List<RecipeOption> options, SelectionPersistencePolicy policy) {
            this.options = options;
            this.policy = policy;
        }

        @Override public Container getContainer() { return null; }
        @Override public Slot getResultSlot() { return null; }
        @Override public int getInputCount() { return 0; }
        @Override public ItemStack getInputStack(int index) { return ItemStack.EMPTY; }
        @Override public List<RecipeOption> findOptions(World world) { return this.options; }
        @Override public boolean select(String recipeKey, World world) {
            this.selectedKey = recipeKey;
            return true;
        }
        @Override public void clearSelection() { this.selectedKey = null; }
        @Nullable @Override public String getSelectedRecipeKey() { return this.selectedKey; }
        @Override public SelectionPersistencePolicy getPersistencePolicy() { return this.policy; }
    }
}
