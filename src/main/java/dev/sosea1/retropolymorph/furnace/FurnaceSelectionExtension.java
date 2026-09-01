package dev.sosea1.retropolymorph.furnace;

import javax.annotation.Nullable;

/** Implemented on vanilla TileEntityFurnace by Mixin. */
public interface FurnaceSelectionExtension {

    @Nullable
    FurnaceSelectionState retropolymorph$peekFurnaceSelectionState();

    FurnaceSelectionState retropolymorph$getOrCreateFurnaceSelectionState();
}
