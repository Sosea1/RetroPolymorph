package com.sosea1.retropolymorph.compat.gregtech;

import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.core.CraftingContext;
import com.sosea1.retropolymorph.core.CraftingMatrixExtension;
import com.sosea1.retropolymorph.core.CraftingRules;
import com.sosea1.retropolymorph.core.RecipeProbe;
import com.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

final class GregTechWorkbenchContext extends CraftingContext {

    private final GregTechWorkbenchReflection.Binding binding;
    private ResourceLocation reconciledId;
    private World reconciledWorld;
    private ItemStack[] reconciledInputs;
    private ItemStack reconciledResult = ItemStack.EMPTY;
    private boolean remoteReady;

    GregTechWorkbenchContext(
            Container container,
            GregTechWorkbenchReflection.Binding binding) {
        super(container, binding.matrix, binding.resultSlot);
        this.binding = binding;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        if (!world.isRemote && this.binding.engine != null && getSelectedRecipeKey() == null) {
            GregTechWorkbenchReflection.refresh(this.binding, null);
        }
        return super.findAllMatches(world);
    }

    @Override
    public boolean select(String recipeKey, World world) {
        if (CraftingRules.isRepairCombination(getRecipeMatrix())) {
            return false;
        }

        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null || !RecipeProbe.matches(recipe, getRecipeMatrix(), world)) {
            return false;
        }

        if (!GregTechWorkbenchReflection.refresh(this.binding, recipe)) {
            return false;
        }
        extension().retropolymorph$getOrCreateRecipeSelectionState().select(recipeId);
        return true;
    }

    @Override
    public void clearSelection() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        if (state == null || !state.hasSelection()) {
            return;
        }

        state.clear();
        GregTechWorkbenchReflection.refresh(this.binding, null);
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        CraftingMatrixExtension ext = extension();
        RecipeSelectionState current = ext.retropolymorph$peekRecipeSelectionState();
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        this.reconciledInputs = null;
        if (recipeId == null) {
            if (current != null) {
                current.clear();
            }
            return;
        }
        ext.retropolymorph$getOrCreateRecipeSelectionState().select(recipeId);
        // A reply can precede ghost-grid packets. Reconciliation below waits for
        // matching inputs before updating GT's native recipe state.
    }

    @Override
    public boolean reconcileRemoteSelection(@Nullable String recipeKey, World world) {
        if (recipeKey == null) {
            if (this.binding.engine != null && (this.reconciledId != null
                    || world != this.reconciledWorld || !sameInputs() || !this.remoteReady)) {
                this.remoteReady = GregTechWorkbenchReflection.refresh(this.binding, null);
                if (!this.remoteReady) {
                    return false;
                }
                captureInputs();
            }
            this.reconciledId = null;
            this.reconciledWorld = world;
            this.reconciledResult = ItemStack.EMPTY;
            this.remoteReady = true;
            // Automatic output belongs to GT's native recipe/availability state,
            // not to a second client-only CraftingManager lookup.
            return true;
        }
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }
        if (!recipeId.equals(this.reconciledId) || world != this.reconciledWorld || !sameInputs()) {
            this.reconciledId = recipeId;
            this.reconciledWorld = world;
            captureInputs();
            IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
            this.remoteReady = recipe != null && !CraftingRules.isRepairCombination(getRecipeMatrix())
                    && RecipeProbe.matches(recipe, getRecipeMatrix(), world);
            this.reconciledResult = this.remoteReady
                    ? RecipeProbe.craftingResult(recipe, getRecipeMatrix()) : ItemStack.EMPTY;
        }
        if (!this.remoteReady) {
            return false;
        }
        extension().retropolymorph$getOrCreateRecipeSelectionState().select(recipeId);
        if (!ItemStack.areItemStacksEqual(getResultSlot().getStack(), this.reconciledResult)) {
            getResultSlot().inventory.setInventorySlotContents(0, this.reconciledResult.copy());
        }
        return true;
    }

    private void captureInputs() {
        this.reconciledInputs = new ItemStack[getInputCount()];
        for (int slot = 0; slot < this.reconciledInputs.length; slot++) {
            this.reconciledInputs[slot] = getInputStack(slot).copy();
        }
    }

    private boolean sameInputs() {
        if (this.reconciledInputs == null) {
            return false;
        }
        for (int slot = 0; slot < this.reconciledInputs.length; slot++) {
            if (!ItemStack.areItemStacksEqual(this.reconciledInputs[slot], getInputStack(slot))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void refreshOutput() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        ResourceLocation recipeId = (state != null && state.hasSelection()) ? state.getSelectedRecipeId() : null;
        IRecipe recipe = recipeId != null ? ForgeRegistries.RECIPES.getValue(recipeId) : null;
        if (recipe != null && !RecipeProbe.matches(recipe, getRecipeMatrix(),
                GregTechWorkbenchReflection.world(this.binding))) {
            state.clear();
            recipe = null;
        }
        GregTechWorkbenchReflection.refresh(this.binding, recipe);
    }

    private CraftingMatrixExtension extension() {
        return (CraftingMatrixExtension) getRecipeMatrix();
    }
}
