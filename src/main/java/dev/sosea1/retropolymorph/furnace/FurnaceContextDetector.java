package dev.sosea1.retropolymorph.furnace;

import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnaceOutput;

import javax.annotation.Nullable;

/** Conservative detector for vanilla-style furnace containers. */
public final class FurnaceContextDetector {

    private FurnaceContextDetector() {
    }

    @Nullable
    public static SelectionContext detect(Container container) {
        Slot resultSlot = null;
        IInventory furnace = null;
        FurnaceSelectionExtension extension = null;

        for (Slot slot : container.inventorySlots) {
            if (!(slot instanceof SlotFurnaceOutput)
                    || !(slot.inventory instanceof FurnaceSelectionExtension)) {
                continue;
            }

            if (resultSlot != null || slot.inventory.getSizeInventory() < 3) {
                return null;
            }

            resultSlot = slot;
            furnace = slot.inventory;
            extension = (FurnaceSelectionExtension) furnace;
        }

        return resultSlot == null
                ? null
                : new FurnaceContext(container, furnace, resultSlot, extension);
    }
}
