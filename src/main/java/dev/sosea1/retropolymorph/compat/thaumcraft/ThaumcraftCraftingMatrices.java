package dev.sosea1.retropolymorph.compat.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

/** Mirrors Thaumcraft's own 3x3 fallback matrix built from Arcane Workbench slots 0..8. */
final class ThaumcraftCraftingMatrices {

    private static final int VANILLA_INPUTS = 9;

    private ThaumcraftCraftingMatrices() {
    }

    static InventoryCrafting copyVanillaMatrix(InventoryCrafting arcaneMatrix) {
        InventoryCrafting vanilla = new InventoryCrafting(new DummyContainer(), 3, 3);
        int limit = Math.min(VANILLA_INPUTS, arcaneMatrix.getSizeInventory());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = arcaneMatrix.getStackInSlot(slot);
            vanilla.setInventorySlotContents(
                    slot,
                    stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return vanilla;
    }

    private static final class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    }
}
