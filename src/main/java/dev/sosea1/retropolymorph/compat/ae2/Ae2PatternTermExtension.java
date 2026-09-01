package dev.sosea1.retropolymorph.compat.ae2;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** Soft bridge implemented by the optional AE2 Pattern Terminal mixin. */
public interface Ae2PatternTermExtension {

    boolean retropolymorph$isPatternCraftingMode();

    @Nullable
    ResourceLocation retropolymorph$getPatternSelectedRecipeId();

    void retropolymorph$setPatternSelectedRecipeId(@Nullable ResourceLocation recipeId);

    void retropolymorph$resetPatternPreviewObservation();

    boolean retropolymorph$wasPatternPreviewObserved();

    ItemStack retropolymorph$refreshPatternOutput();
}
