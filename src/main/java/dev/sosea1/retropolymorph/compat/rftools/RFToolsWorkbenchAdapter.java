package dev.sosea1.retropolymorph.compat.rftools;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/** Detects the manual Workbench from RFTools Control by its injected tile contract. */
public final class RFToolsWorkbenchAdapter implements RecipeSelectionAdapter {

    public static final RFToolsWorkbenchAdapter INSTANCE = new RFToolsWorkbenchAdapter();

    private static final int INPUT_COUNT = 9;
    private static final int OUTPUT_INDEX = 9;

    private RFToolsWorkbenchAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        Slot[] inputs = new Slot[INPUT_COUNT];
        Slot output = null;
        IInventory workbench = null;

        for (Slot slot : container.inventorySlots) {
            if (!(slot.inventory instanceof RFToolsWorkbenchSelectionExtension)) {
                continue;
            }

            if (workbench == null) {
                workbench = slot.inventory;
            } else if (workbench != slot.inventory) {
                return AdapterDetectionResult.blockFallback();
            }

            int index = slot.getSlotIndex();
            if (index >= 0 && index < INPUT_COUNT) {
                if (inputs[index] != null) {
                    return AdapterDetectionResult.blockFallback();
                }
                inputs[index] = slot;
            } else if (index == OUTPUT_INDEX) {
                if (output != null) {
                    return AdapterDetectionResult.blockFallback();
                }
                output = slot;
            }
        }

        if (workbench == null) {
            return AdapterDetectionResult.miss();
        }
        if (output == null) {
            return AdapterDetectionResult.blockFallback();
        }
        for (Slot input : inputs) {
            if (input == null) {
                return AdapterDetectionResult.blockFallback();
            }
        }

        return AdapterDetectionResult.match(new RFToolsWorkbenchContext(
                container,
                (RFToolsWorkbenchSelectionExtension) workbench,
                inputs,
                output));
    }
}
