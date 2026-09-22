package dev.sosea1.retropolymorph.compat.extrautils2;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** Dependency-free bridge injected into the two Extra Utilities 2 crafter tiles. */
public interface ExtraUtilities2SelectionAccess {

    @Nullable
    ResourceLocation retropolymorph$getExtraUtilities2Recipe();

    void retropolymorph$setExtraUtilities2Recipe(@Nullable ResourceLocation recipeId);
}
