package com.sosea1.retropolymorph.core;

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
    private boolean craftTransactionOpen;

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
        this.craftTransactionOpen = false;
    }

    /** Keeps selection alive while SlotCrafting temporarily mutates its matrix. */
    public void beginCraftTransaction() {
        this.craftTransactionOpen = this.selectedRecipeId != null;
    }

    public void endCraftTransaction(InventoryCrafting matrix, World world) {
        if (!this.craftTransactionOpen) {
            return;
        }
        this.craftTransactionOpen = false;
        if (this.selectedRecipeId == null) {
            return;
        }

        IRecipe candidate = ForgeRegistries.RECIPES.getValue(this.selectedRecipeId);
        if (candidate == null
                || CraftingRules.isRepairCombination(matrix)
                || !RecipeProbe.matches(candidate, matrix, world)) {
            if (!com.sosea1.retropolymorph.config.PolymorphConfig.isRememberPlayerChoices()) {
                clear();
            }
        }
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

        if (RecipeProbe.matches(candidate, matrix, world)) {
            return candidate;
        }

        return null;
    }
}
