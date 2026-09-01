package dev.sosea1.retropolymorph.compat.extendedcrafting;

import dev.sosea1.retropolymorph.core.CraftingRules;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Shared selected-recipe validation used by both Extended Crafting hooks. */
public final class ExtendedTableSelectionResolver {

    private ExtendedTableSelectionResolver() {
    }

    @Nullable
    public static IRecipe resolve(
            InventoryCrafting matrix,
            World world,
            ExtendedTableRecipeManagerBridge manager) {
        if (!(matrix instanceof ExtendedTableMatrixExtension)) {
            return null;
        }

        ExtendedTableMatrixExtension extension = (ExtendedTableMatrixExtension) matrix;
        if (CraftingRules.isRepairCombination(matrix)) {
            extension.retropolymorph$clearTableRecipe();
            return null;
        }

        int index = extension.retropolymorph$getSelectedTableRecipeIndex();
        IRecipe expected = extension.retropolymorph$getSelectedTableRecipe();
        if (index < 0 || expected == null) {
            return null;
        }

        List<IRecipe> recipes = manager.retropolymorph$getTableRecipes();
        if (index >= recipes.size() || recipes.get(index) != expected) {
            extension.retropolymorph$clearTableRecipe();
            return null;
        }

        if (!expected.matches(matrix, world)) {
            extension.retropolymorph$clearTableRecipe();
            return null;
        }

        extension.retropolymorph$markTableResolutionObserved();
        return expected;
    }
}
