package dev.sosea1.retropolymorph.compat.enderio;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** Full Ender IO Crafter integration: ghost preview and real automatic craft share one selection. */
public final class EnderIoCrafterAdapter implements RecipeSelectionAdapter {

    public static final EnderIoCrafterAdapter INSTANCE = new EnderIoCrafterAdapter();

    private EnderIoCrafterAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!EnderIoCrafterReflection.isCrafterContainer(container)) {
            return AdapterDetectionResult.miss();
        }

        TileEntity tile = EnderIoCrafterReflection.getTile(container);
        IInventory grid = EnderIoCrafterReflection.getCraftingGrid(tile);
        Slot result = findRealOutput(container);
        if (tile == null || grid == null || grid.getSizeInventory() < 10 || result == null) {
            return AdapterDetectionResult.blockFallback();
        }
        return AdapterDetectionResult.match(new EnderIoCrafterContext(container, tile, grid, result));
    }

    @Nullable
    private static Slot findRealOutput(Container container) {
        Slot fallback = null;
        for (Slot slot : container.inventorySlots) {
            if (slot.xPos == 172 && slot.yPos == 34) {
                return slot;
            }
            if (slot.xPos >= 160 && slot.yPos >= 20 && slot.yPos <= 50) {
                fallback = slot;
            }
        }
        return fallback;
    }
}
