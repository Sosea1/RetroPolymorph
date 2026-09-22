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

    /**
     * Keeps a selected recipe alive across SlotCrafting's transient matrix
     * mutations while one craft is consuming inputs and returning remainders.
     */
    public void beginCraftTransaction() {
        // SlotCrafting#onTake is not a nested transaction. Re-opening the
        // window also recovers cleanly if a third-party override aborted the
        // previous onTake before our RETURN hook could run.
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
            clear();
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

        // SlotCrafting mutates the matrix one slot at a time before placing
        // crafting remainders. During that short transaction a perfectly valid
        // selected recipe may temporarily stop matching. Preserve the id until
        // the final post-remainder matrix can be validated.
        if (this.craftTransactionOpen) {
            return null;
        }

        clear();
        return null;
    }
}
