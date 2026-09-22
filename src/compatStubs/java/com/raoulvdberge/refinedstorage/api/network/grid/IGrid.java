package com.raoulvdberge.refinedstorage.api.network.grid;

import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nullable;

/** Compile-only signature stub; never packaged. */
public interface IGrid {
    GridType getGridType();

    @Nullable
    InventoryCrafting getCraftingMatrix();

    @Nullable
    InventoryCraftResult getCraftingResult();

    void onCraftingMatrixChanged();
}
