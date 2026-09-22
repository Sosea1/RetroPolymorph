package dev.sosea1.retropolymorph.compat.cyclic;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.CraftingContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/** Bridges Cyclic's persistent workbench inventories to the normal recipe selector. */
public final class CyclicWorkbenchAdapter implements RecipeSelectionAdapter {

    public static final CyclicWorkbenchAdapter INSTANCE = new CyclicWorkbenchAdapter();

    private CyclicWorkbenchAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!(container instanceof CyclicWorkbenchAccess)) {
            return AdapterDetectionResult.miss();
        }

        CyclicWorkbenchAccess access = (CyclicWorkbenchAccess) container;
        InventoryCrafting matrix = access.retropolymorph$getCyclicCraftMatrix();
        IInventory result = access.retropolymorph$getCyclicCraftResult();
        if (matrix == null || result == null) {
            return AdapterDetectionResult.blockFallback();
        }

        Slot resultSlot = null;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory != result) {
                continue;
            }
            if (resultSlot != null) {
                return AdapterDetectionResult.blockFallback();
            }
            resultSlot = slot;
        }

        return resultSlot == null
                ? AdapterDetectionResult.blockFallback()
                : AdapterDetectionResult.match(new CraftingContext(container, matrix, resultSlot));
    }
}
