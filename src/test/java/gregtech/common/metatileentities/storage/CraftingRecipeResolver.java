package gregtech.common.metatileentities.storage;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;

/** Original GTCE resolver members used by the compatibility binding. */
public class CraftingRecipeResolver {
    public final InventoryCrafting inventoryCrafting;
    public IInventory result;
    public IRecipe cachedRecipe;
    public final net.minecraftforge.items.ItemStackHandler craftingGrid = new net.minecraftforge.items.ItemStackHandler(9);
    private final Object itemSourceList = new Object();
    public CachedRecipeData cachedRecipeData;
    public CraftingRecipeResolver(InventoryCrafting matrix, IInventory result) {
        this.inventoryCrafting = matrix;
        this.result = result;
        for (int slot = 0; slot < 9; slot++) {
            craftingGrid.setStackInSlot(slot, matrix.getStackInSlot(slot).copy());
        }
    }
    public IInventory getCraftingResultInventory() { return result; }
    public Object getItemSourceList() { return itemSourceList; }
    public boolean updateInventoryCrafting() {
        for (int slot = 0; slot < 9; slot++) {
            inventoryCrafting.setInventorySlotContents(slot, craftingGrid.getStackInSlot(slot).copy());
        }
        return true;
    }
    public void updateCurrentRecipe() { }

    public static class CachedRecipeData {
        public final IRecipe recipe;
        public final net.minecraft.item.ItemStack expectedOutput;
        public final InventoryCrafting inventory = new InventoryCrafting(new net.minecraft.inventory.Container() {
            @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return true; }
        }, 3, 3);
        public boolean matched;
        public CachedRecipeData(Object sourceList, IRecipe recipe, net.minecraft.item.ItemStack output) {
            this.recipe = recipe;
            this.expectedOutput = output.copy();
        }
        public boolean attemptMatchRecipe() { matched = true; return true; }
    }
}
