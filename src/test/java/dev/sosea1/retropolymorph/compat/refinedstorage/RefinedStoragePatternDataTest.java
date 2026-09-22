package dev.sosea1.retropolymorph.compat.refinedstorage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RefinedStoragePatternDataTest {

    @BeforeAll
    public static void setup() {
        Bootstrap.register();
    }

    @Test
    public void roundTripsSelectedRecipeWithoutTouchingTheItem() {
        Item item = Items.PAPER;
        ItemStack pattern = new ItemStack(item);
        ResourceLocation recipe = new ResourceLocation("example", "chosen_recipe");

        RefinedStoragePatternData.writeSelectedRecipe(pattern, recipe);

        assertEquals(recipe, RefinedStoragePatternData.readSelectedRecipe(pattern));
        assertEquals(item, pattern.getItem());
    }

    @Test
    public void clearingRecipeRemovesOnlyRetroPolymorphMetadata() {
        Item item = Items.PAPER;
        ItemStack pattern = new ItemStack(item);
        pattern.setTagInfo("OtherMod", new NBTTagCompound());
        RefinedStoragePatternData.writeSelectedRecipe(
                pattern, new ResourceLocation("example", "chosen_recipe"));

        RefinedStoragePatternData.writeSelectedRecipe(pattern, null);

        assertNull(RefinedStoragePatternData.readSelectedRecipe(pattern));
        assertTrue(pattern.getTagCompound().hasKey("OtherMod"));
    }
    @Test
    public void waitsUntilAllEncodedInputsArePresent() {
        ItemStack itemA = new ItemStack(Items.APPLE);
        ItemStack itemB = new ItemStack(Items.STICK);
        ItemStack pattern = new ItemStack(Items.PAPER);
        NBTTagCompound root = new NBTTagCompound();
        root.setBoolean("Processing", false);
        root.setTag("Input_0", itemA.serializeNBT());
        root.setTag("Input_8", itemB.serializeNBT());
        pattern.setTagCompound(root);

        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        matrix.setInventorySlotContents(0, itemA.copy());
        assertFalse(RefinedStoragePatternData.matchesEncodedInputs(pattern, matrix));

        matrix.setInventorySlotContents(8, itemB.copy());
        assertTrue(RefinedStoragePatternData.matchesEncodedInputs(pattern, matrix));
    }

    @Test
    public void neverTreatsProcessingPatternAsCraftingPattern() {
        ItemStack pattern = new ItemStack(Items.PAPER);
        NBTTagCompound root = new NBTTagCompound();
        root.setBoolean("Processing", true);
        pattern.setTagCompound(root);

        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        assertFalse(RefinedStoragePatternData.isRegularCraftingPattern(pattern));
        assertFalse(RefinedStoragePatternData.matchesEncodedInputs(pattern, matrix));
    }

    private static final class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    }

}
