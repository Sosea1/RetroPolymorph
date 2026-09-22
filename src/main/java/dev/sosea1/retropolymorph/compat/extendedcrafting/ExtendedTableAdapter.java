package dev.sosea1.retropolymorph.compat.extendedcrafting;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
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
    public AdapterDetectionResult probe(Container container) {
        ExtendedTableRecipeManagerBridge manager =
                ExtendedCraftingBridgeRegistry.getRecipeManager();
        if (manager == null) {
            return AdapterDetectionResult.miss();
        }

        Slot resultSlot = null;
        InventoryCrafting matrix = null;
        for (Slot slot : container.inventorySlots) {
            if (!(slot instanceof ExtendedTableResultSlot)) {
                continue;
            }
            if (resultSlot != null) {
                return AdapterDetectionResult.blockFallback();
            }

            resultSlot = slot;
            matrix = ((ExtendedTableResultSlot) slot).retropolymorph$getExtendedCraftingMatrix();
        }

        if (resultSlot == null || !(matrix instanceof ExtendedTableMatrixExtension)) {
            return AdapterDetectionResult.miss();
        }

        return AdapterDetectionResult.match(new ExtendedTableContext(container, matrix, resultSlot, manager));
    }
}
