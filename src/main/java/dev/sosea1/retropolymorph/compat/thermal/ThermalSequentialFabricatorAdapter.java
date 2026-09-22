package dev.sosea1.retropolymorph.compat.thermal;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** Thermal Expansion 1.12 Sequential Fabricator ghost-grid + tile recipe bridge. */
public final class ThermalSequentialFabricatorAdapter implements RecipeSelectionAdapter {

    public static final ThermalSequentialFabricatorAdapter INSTANCE =
            new ThermalSequentialFabricatorAdapter();

    private ThermalSequentialFabricatorAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!ThermalSequentialFabricatorLayout.isTarget(container)) {
            return AdapterDetectionResult.miss();
        }
        InventoryCrafting matrix = ThermalSequentialFabricatorLayout.findMatrix(container);
        Slot result = ThermalSequentialFabricatorLayout.findResultSlot(container);
        TileEntity tile = ThermalSequentialFabricatorLayout.findTile(container);
        if (matrix == null || result == null || tile == null) {
            return AdapterDetectionResult.blockFallback();
        }
        return AdapterDetectionResult.match(new ThermalSequentialFabricatorContext(container, matrix, result, tile));
    }
}
