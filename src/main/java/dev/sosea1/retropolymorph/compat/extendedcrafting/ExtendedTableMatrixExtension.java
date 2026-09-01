package dev.sosea1.retropolymorph.compat.extendedcrafting;

import net.minecraft.item.crafting.IRecipe;

import javax.annotation.Nullable;

/** Soft state attached to Extended Crafting's TableCrafting matrix. */
public interface ExtendedTableMatrixExtension {

    int retropolymorph$getSelectedTableRecipeIndex();

    @Nullable
    IRecipe retropolymorph$getSelectedTableRecipe();

    void retropolymorph$selectTableRecipe(int index, IRecipe recipe);

    void retropolymorph$clearTableRecipe();

    void retropolymorph$markTableResolutionObserved();

    boolean retropolymorph$wasTableResolutionObserved();
}
