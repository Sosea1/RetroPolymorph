package dev.sosea1.retropolymorph.compat.mekanism;

import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nullable;

/** Dependency-free bridge injected into Mekanism's Formulaic Assemblicator tile. */
public interface MekanismFormulaicTileAccess {

    @Nullable
    InventoryCrafting retropolymorph$getFormulaicDummyMatrix();

    boolean retropolymorph$hasFormula();

    void retropolymorph$invalidateAndRecalculateRecipe();
}
