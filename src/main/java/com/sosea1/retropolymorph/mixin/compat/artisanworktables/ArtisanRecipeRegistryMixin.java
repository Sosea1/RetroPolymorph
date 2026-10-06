package com.sosea1.retropolymorph.mixin.compat.artisanworktables;

import com.sosea1.retropolymorph.compat.artisanworktables.ArtisanRecipeSelectionBridge;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Pseudo
@Mixin(
        targets = "com.codetaylor.mc.artisanworktables.api.internal.recipe.RecipeRegistry",
        remap = false)
public abstract class ArtisanRecipeRegistryMixin {

    @Redirect(
            method = "findRecipe",
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"),
            remap = false,
            require = 0)
    private Object retropolymorph$selectedRecipeFirst(List<?> recipes, int index) {
        return PolymorphConfig.isIntegrationArtisanWorktablesEnabled()
                ? ArtisanRecipeSelectionBridge.get(recipes, index)
                : recipes.get(index);
    }

    @Redirect(
            method = "findRecipe",
            at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;"),
            remap = false,
            require = 0)
    private Object retropolymorph$removeFromVirtualOrder(List<?> recipes, int index) {
        return PolymorphConfig.isIntegrationArtisanWorktablesEnabled()
                ? ArtisanRecipeSelectionBridge.remove(recipes, index)
                : recipes.remove(index);
    }
}
