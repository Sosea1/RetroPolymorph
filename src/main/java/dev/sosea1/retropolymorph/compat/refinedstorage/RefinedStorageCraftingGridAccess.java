package dev.sosea1.retropolymorph.compat.refinedstorage;

import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nullable;

/**
 * Dependency-free runtime contract injected into Refined Storage's ContainerGrid.
 *
 * Keeping RS types out of the core adapter means RetroPolymorph remains fully
 * optional with respect to Refined Storage and can be classloaded without it.
 */
public interface RefinedStorageCraftingGridAccess {

    boolean retropolymorph$isRefinedStorageCraftingGrid();

    boolean retropolymorph$isRefinedStoragePatternGrid();

    boolean retropolymorph$isRefinedStorageProcessingPattern();

    @Nullable
    InventoryCrafting retropolymorph$getRefinedStorageCraftingMatrix();

    void retropolymorph$refreshRefinedStorageCraftingMatrix();
}
