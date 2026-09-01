package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * A detected user-facing crafting context backed by a real InventoryCrafting
 * and the normal Forge recipe registry.
 */
public final class CraftingContext implements RecipeSelectionContext {

    private final Container container;
    private final InventoryCrafting matrix;
    private final Slot resultSlot;

    public CraftingContext(Container container, InventoryCrafting matrix, Slot resultSlot) {
        this.container = container;
        this.matrix = matrix;
        this.resultSlot = resultSlot;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        return this.matrix;
    }

    @Override
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        if (CraftingRules.isRepairCombination(this.matrix)) {
            return Collections.emptyList();
        }
        return RecipeResolver.findAllMatches(this.matrix, world);
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation recipeId = recipe.getRegistryName();
        return recipeId == null ? null : recipeId.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        if (CraftingRules.isRepairCombination(this.matrix)) {
            return false;
        }

        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null || !recipe.matches(this.matrix, world)) {
            return false;
        }

        RecipeSelectionState state = extension().retropolymorph$getOrCreateRecipeSelectionState();
        state.select(recipeId);
        refreshOutput();
        return true;
    }

    @Override
    public void clearSelection() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        if (state == null || !state.hasSelection()) {
            return;
        }

        state.clear();
        refreshOutput();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        ResourceLocation recipeId = state == null ? null : state.getSelectedRecipeId();
        return recipeId == null ? null : recipeId.toString();
    }

    public void refreshOutput() {
        this.container.onCraftMatrixChanged(this.matrix);
    }

    private CraftingMatrixExtension extension() {
        return (CraftingMatrixExtension) this.matrix;
    }
}
