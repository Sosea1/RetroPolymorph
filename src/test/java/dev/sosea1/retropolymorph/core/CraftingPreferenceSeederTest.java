package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.preference.InputFingerprint;
import dev.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import net.minecraft.init.Bootstrap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CraftingPreferenceSeederTest {

    private static Item testItem;

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        testItem = new Item();
    }

    @Test
    public void inputAliasRestoresSelectionWithoutEnumeratingOptions() {
        StubContext context = new StubContext(SelectionPersistencePolicy.PLAYER_PERSISTENT);
        NBTTagCompound playerData = new NBTTagCompound();
        String inputFingerprint = InputFingerprint.create(context);
        PlayerRecipePreferences.rememberInput(playerData, inputFingerprint, "test:emerald");

        CraftingPreferenceSeeder.preseed(null, playerData, context);

        assertEquals("test:emerald", context.getSelectedRecipeKey());
        assertEquals(0, context.findOptionsCalls);
    }

    @Test
    public void ownerOnlyContextNeverConsumesPlayerInputAlias() {
        StubContext context = new StubContext(SelectionPersistencePolicy.OWNER_ONLY);
        NBTTagCompound playerData = new NBTTagCompound();
        String inputFingerprint = InputFingerprint.create(context);
        PlayerRecipePreferences.rememberInput(playerData, inputFingerprint, "test:emerald");

        CraftingPreferenceSeeder.preseed(null, playerData, context);

        assertNull(context.getSelectedRecipeKey());
        assertEquals(0, context.findOptionsCalls);
    }

    @Test
    public void genericPreseedOnlyAcceptsAMatrixOwnedByTheContainerSlots() {
        TestContainer owner = new TestContainer();
        InventoryCrafting owned = new InventoryCrafting(owner, 3, 3);
        owner.addTestSlot(new Slot(owned, 0, 0, 0));
        InventoryCrafting scratch = new InventoryCrafting(new TestContainer(), 3, 3);

        assertTrue(CraftingPreferenceSeeder.isMatrixOwnedBy(owner, owned));
        assertFalse(CraftingPreferenceSeeder.isMatrixOwnedBy(owner, scratch));
    }

    private static final class TestContainer extends Container {
        void addTestSlot(Slot slot) { this.addSlotToContainer(slot); }
        @Override public boolean canInteractWith(EntityPlayer playerIn) { return true; }
    }

    private static final class StubContext implements SelectionContext {
        private final SelectionPersistencePolicy persistence;
        private final ItemStack input = new ItemStack(testItem, 32, 0);
        private int findOptionsCalls;
        @Nullable
        private String selected;

        private StubContext(SelectionPersistencePolicy persistence) {
            this.persistence = persistence;
        }

        @Override public Container getContainer() { return null; }
        @Override public Slot getResultSlot() { return null; }
        @Override public int getInputCount() { return 1; }
        @Override public ItemStack getInputStack(int index) { return index == 0 ? this.input : ItemStack.EMPTY; }
        @Override public List<RecipeOption> findOptions(World world) {
            this.findOptionsCalls++;
            throw new AssertionError("preseed must not enumerate recipe options");
        }
        @Override public boolean select(String recipeKey, World world) {
            this.selected = recipeKey;
            return true;
        }
        @Override public void clearSelection() { this.selected = null; }
        @Nullable @Override public String getSelectedRecipeKey() { return this.selected; }
        @Override public SelectionPersistencePolicy getPersistencePolicy() { return this.persistence; }
    }
}
