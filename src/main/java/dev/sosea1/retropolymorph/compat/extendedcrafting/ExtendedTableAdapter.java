package dev.sosea1.retropolymorph.compat.extendedcrafting;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/** Focused adapter for Extended Crafting 1.x table recipe pools. */
public final class ExtendedTableAdapter implements RecipeSelectionAdapter {

    public static final ExtendedTableAdapter INSTANCE = new ExtendedTableAdapter();

    private ExtendedTableAdapter() {
    }

    @Override
    @Nullable
    public RecipeSelectionContext createContext(Container container) {
        ExtendedTableRecipeManagerBridge manager =
                ExtendedCraftingBridgeRegistry.getRecipeManager();
        if (manager == null) {
            return null;
        }

        Slot resultSlot = null;
        InventoryCrafting matrix = null;
        for (Slot slot : container.inventorySlots) {
            if (!(slot instanceof ExtendedTableResultSlot)) {
                continue;
            }
            if (resultSlot != null) {
                return null;
            }

            resultSlot = slot;
            matrix = ((ExtendedTableResultSlot) slot).retropolymorph$getExtendedCraftingMatrix();
        }

        if (resultSlot == null
                || !(matrix instanceof ExtendedTableMatrixExtension)) {
            return null;
        }

        return new ExtendedTableContext(container, matrix, resultSlot, manager);
    }
}
