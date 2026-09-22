package dev.sosea1.retropolymorph.compat.mekanism;

import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** Dependency-free bridge injected into Mekanism's Formulaic Assemblicator container. */
public interface MekanismFormulaicAssemblicatorAccess {

    @Nullable
    TileEntity retropolymorph$getFormulaicAssemblicatorTile();
}
