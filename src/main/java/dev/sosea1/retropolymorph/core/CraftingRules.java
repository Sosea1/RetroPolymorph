package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

/** Vanilla crafting rules that are not represented by ordinary IRecipe entries. */
public final class CraftingRules {

    private CraftingRules() {
    }

    /**
     * Minecraft 1.12 resolves the two-item repair shortcut before normal recipe
     * lookup. Retro Polymorph must not let an explicit registry recipe bypass that
     * higher-priority vanilla behavior.
     */
    public static boolean isRepairCombination(InventoryCrafting matrix) {
        ItemStack first = ItemStack.EMPTY;
        ItemStack second = ItemStack.EMPTY;
        int occupied = 0;

        for (int slot = 0; slot < matrix.getSizeInventory(); slot++) {
            ItemStack stack = matrix.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (occupied == 0) {
                first = stack;
            } else if (occupied == 1) {
                second = stack;
            }
            occupied++;
            if (occupied > 2) {
                return false;
            }
        }

        return occupied == 2
                && first.getItem() == second.getItem()
                && first.getCount() == 1
                && second.getCount() == 1
                && first.getItem().isRepairable();
    }
}
