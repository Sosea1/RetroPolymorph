package dev.sosea1.retropolymorph.compat.gregtech;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.core.CraftingContext;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingRules;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

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

        RecipeSelectionState state = extension().retropolymorph$getOrCreateRecipeSelectionState();
        state.select(recipeId);
        GregTechWorkbenchReflection.refresh(this.binding, recipe);
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
        // matching inputs; auto replies must leave GT's native output untouched.
    }

    @Override
    public boolean reconcileRemoteSelection(@Nullable String recipeKey, World world) {
        if (recipeKey == null) {
            return true;
        }
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }
        if (!recipeId.equals(this.reconciledId) || world != this.reconciledWorld || !sameInputs()) {
            this.reconciledId = recipeId;
            this.reconciledWorld = world;
            this.reconciledInputs = new ItemStack[getInputCount()];
            for (int slot = 0; slot < this.reconciledInputs.length; slot++) {
                this.reconciledInputs[slot] = getInputStack(slot).copy();
            }
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
