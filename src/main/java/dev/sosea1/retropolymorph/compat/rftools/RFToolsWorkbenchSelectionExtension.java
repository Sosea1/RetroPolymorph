package dev.sosea1.retropolymorph.compat.rftools;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** Selection state injected into the RFTools Control manual Workbench tile. */
public interface RFToolsWorkbenchSelectionExtension {

    @Nullable
    ResourceLocation retropolymorph$getWorkbenchRecipeId();

    void retropolymorph$setWorkbenchRecipeId(@Nullable ResourceLocation recipeId);

    void retropolymorph$refreshWorkbenchRecipe();

    boolean retropolymorph$wasWorkbenchSelectionObserved();
}
