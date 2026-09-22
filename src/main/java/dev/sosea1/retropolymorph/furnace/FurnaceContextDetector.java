package dev.sosea1.retropolymorph.furnace;

import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnaceOutput;
import net.minecraft.tileentity.TileEntityFurnace;

import javax.annotation.Nullable;

/** Conservative detector for vanilla-style furnace containers. */
public final class FurnaceContextDetector {

    private FurnaceContextDetector() {
    }

    @Nullable
    public static SelectionContext detect(Container container) {
        if (container == null) {
            return null;
        }

        boolean vanillaContainer = container.getClass() == ContainerFurnace.class;
        Slot resultSlot = null;
        IInventory furnace = null;

        for (Slot slot : container.inventorySlots) {
            if (!(slot instanceof SlotFurnaceOutput)) {
                continue;
            }

            if (resultSlot != null
                    || (!vanillaContainer && !(slot.inventory instanceof TileEntityFurnace))
                    || slot.inventory.getSizeInventory() < 3) {
                return null;
            }

            resultSlot = slot;
            furnace = slot.inventory;
        }

        return resultSlot == null
                ? null
                : new FurnaceContext(container, furnace, resultSlot);
    }
}
