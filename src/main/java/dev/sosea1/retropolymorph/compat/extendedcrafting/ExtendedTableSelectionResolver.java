package dev.sosea1.retropolymorph.compat.extendedcrafting;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingRules;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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
        if (index >= 0 && expected != null) {
            List<IRecipe> recipes = manager.retropolymorph$getTableRecipes();
            if (index >= recipes.size() || recipes.get(index) != expected) {
                extension.retropolymorph$clearTableRecipe();
                return null;
            }

            if (!RecipeProbe.matches(expected, matrix, world)) {
                extension.retropolymorph$clearTableRecipe();
                return null;
            }

            extension.retropolymorph$markTableResolutionObserved();
            return expected;
        }

        // Forge fallback resolution (e.g. standard 3x3 recipes on Basic Table)
        if (matrix instanceof CraftingMatrixExtension) {
            CraftingMatrixExtension matrixExt = (CraftingMatrixExtension) matrix;
            RecipeSelectionState forgeState = matrixExt.retropolymorph$peekRecipeSelectionState();
            if (forgeState != null && forgeState.hasSelection()) {
                return forgeState.resolveSelectedForOutput(matrix, world);
            }
        }

        return null;
    }
}

