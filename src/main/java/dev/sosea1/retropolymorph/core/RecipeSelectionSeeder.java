package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Seeds an already server-authorized Forge recipe id into a temporary crafting
 * matrix created by a compatibility target.
 *
 * The normal CraftingManager hook still revalidates registry presence,
 * vanilla repair priority and {@code IRecipe.matches()} before using it. This
 * helper therefore transfers intent only; it never bypasses recipe validation.
 */
public final class RecipeSelectionSeeder {

    private RecipeSelectionSeeder() {
    }

    public static void seed(
            InventoryCrafting matrix,
            @Nullable ResourceLocation selectedRecipeId) {
        if (selectedRecipeId == null || !(matrix instanceof CraftingMatrixExtension)) {
            return;
        }

        CraftingMatrixExtension extension = (CraftingMatrixExtension) matrix;
        extension.retropolymorph$getOrCreateRecipeSelectionState().select(selectedRecipeId);
    }

    public static boolean wasOutputResolutionObserved(
            InventoryCrafting matrix,
            @Nullable ResourceLocation expectedRecipeId) {
        if (expectedRecipeId == null || !(matrix instanceof CraftingMatrixExtension)) {
            return false;
        }

        RecipeSelectionState state = ((CraftingMatrixExtension) matrix)
                .retropolymorph$peekRecipeSelectionState();
        return state != null
                && expectedRecipeId.equals(state.getSelectedRecipeId())
                && state.wasResolutionObserved();
    }
}
