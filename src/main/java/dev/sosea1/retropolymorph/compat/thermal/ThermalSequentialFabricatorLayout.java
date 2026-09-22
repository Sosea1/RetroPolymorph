package dev.sosea1.retropolymorph.compat.thermal;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** Dependency-free discovery of Thermal 1.12's ghost crafting surface. */
final class ThermalSequentialFabricatorLayout {

    static final String CONTAINER_CLASS =
            "cofh.thermalexpansion.gui.container.machine.ContainerCrafter";

    private ThermalSequentialFabricatorLayout() {
    }

    static boolean isTarget(Container container) {
        return container != null && CONTAINER_CLASS.equals(container.getClass().getName());
    }

    @Nullable
    static InventoryCrafting findMatrix(Container container) {
        if (container == null) {
            return null;
        }
        InventoryCrafting found = null;
        int matchingSlots = 0;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof InventoryCrafting) {
                InventoryCrafting matrix = (InventoryCrafting) slot.inventory;
                if (matrix.getSizeInventory() != 9) {
                    continue;
                }
                if (found == null) {
                    found = matrix;
                }
                if (found == matrix) {
                    matchingSlots++;
                }
            }
        }
        return matchingSlots == 9 ? found : null;
    }

    @Nullable
    static Slot findResultSlot(Container container) {
        if (container == null) {
            return null;
        }
        for (Slot slot : container.inventorySlots) {
            if (slot instanceof SlotCrafting && slot.xPos == 125 && slot.yPos == 48) {
                return slot;
            }
        }
        return null;
    }

    @Nullable
    static TileEntity findTile(Container container) {
        if (container == null) {
            return null;
        }
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof TileEntity) {
                TileEntity tile = (TileEntity) slot.inventory;
                if ("cofh.thermalexpansion.block.machine.TileCrafter"
                        .equals(tile.getClass().getName())) {
                    return tile;
                }
            }
        }
        return null;
    }
}
