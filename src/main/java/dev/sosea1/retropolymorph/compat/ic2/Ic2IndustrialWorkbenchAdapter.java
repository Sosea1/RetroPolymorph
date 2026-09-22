package dev.sosea1.retropolymorph.compat.ic2;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.CraftingContext;
import dev.sosea1.retropolymorph.mixin.compat.ic2.Ic2IndustrialWorkbenchAccess;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/** Bridges IC2's manual Industrial Workbench to the normal Forge recipe path. */
public final class Ic2IndustrialWorkbenchAdapter implements RecipeSelectionAdapter {

    public static final Ic2IndustrialWorkbenchAdapter INSTANCE =
            new Ic2IndustrialWorkbenchAdapter();

    private Ic2IndustrialWorkbenchAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!(container instanceof Ic2IndustrialWorkbenchAccess)) {
            return AdapterDetectionResult.miss();
        }

        Ic2IndustrialWorkbenchAccess access = (Ic2IndustrialWorkbenchAccess) container;
        InventoryCrafting matrix = access.retropolymorph$getCraftMatrix();
        IInventory result = access.retropolymorph$getCraftResult();
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
