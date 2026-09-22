package dev.sosea1.retropolymorph.compat.cyclic;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** Accessor for Cyclic TileEntityCrafter selected recipe. */
public interface CyclicSelectionAccess {

    @Nullable
    ResourceLocation retropolymorph$getSelectedRecipeId();

    void retropolymorph$setSelectedRecipeId(@Nullable ResourceLocation recipeId);
}
