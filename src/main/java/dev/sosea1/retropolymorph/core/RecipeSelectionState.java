package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

public final class RecipeSelectionState {

    @Nullable
    private ResourceLocation selectedRecipeId;
    private boolean resolutionObserved;

    public boolean hasSelection() {
        return this.selectedRecipeId != null;
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return this.selectedRecipeId;
    }

    public void select(ResourceLocation recipeId) {
        this.selectedRecipeId = recipeId;
        this.resolutionObserved = false;
    }

    public void clear() {
        this.selectedRecipeId = null;
        this.resolutionObserved = false;
    }

    public boolean wasResolutionObserved() {
        return this.resolutionObserved;
    }

    @Nullable
    public IRecipe resolveSelectedForOutput(InventoryCrafting matrix, World world) {
        IRecipe recipe = resolveSelected(matrix, world);
        if (recipe != null) {
            this.resolutionObserved = true;
        }
        return recipe;
    }

    @Nullable
    public IRecipe resolveSelected(InventoryCrafting matrix, World world) {
        if (this.selectedRecipeId == null) {
            return null;
        }

        IRecipe candidate = ForgeRegistries.RECIPES.getValue(this.selectedRecipeId);
        if (candidate == null) {
            clear();
            return null;
        }

        if (CraftingRules.isRepairCombination(matrix)) {
            clear();
            return null;
        }

        if (candidate.matches(matrix, world)) {
            return candidate;
        }

        clear();
        return null;
    }
}
