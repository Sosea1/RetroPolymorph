package dev.sosea1.retropolymorph.compat.extendedcrafting;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import javax.annotation.Nullable;

/**
 * Shared helper for resolving effective recipe output and validating expected output
 * fingerprints across Ender Crafter surfaces and mixins.
 */
public final class EnderCrafterRecipeIdentity {

    private EnderCrafterRecipeIdentity() {
    }

    /**
     * Resolves the effective output of an Ender Crafter recipe:
     * 1. Safely checks static {@code recipe.getRecipeOutput()}.
     * 2. If empty or unresolvable, falls back to dynamic evaluation via {@link RecipeProbe#craftingResult(IRecipe, InventoryCrafting)}.
     * 3. Guarantees non-null {@link ItemStack#EMPTY} return.
     */
    public static ItemStack resolveEffectiveOutput(@Nullable IRecipe recipe, @Nullable InventoryCrafting matrix) {
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        ItemStack output = ItemStack.EMPTY;
        try {
            output = recipe.getRecipeOutput();
        } catch (RuntimeException | LinkageError ignored) {
        }
        if (output == null || output.isEmpty()) {
            if (matrix != null) {
                output = RecipeProbe.craftingResult(recipe, matrix);
            }
        }
        return output != null ? output : ItemStack.EMPTY;
    }

    /**
     * Checks whether a recipe's effective output matches an expected output fingerprint.
     */
    public static boolean matchesExpectedOutput(
            @Nullable IRecipe recipe,
            @Nullable InventoryCrafting matrix,
            @Nullable ItemStack expectedOutput) {
        if (recipe == null || expectedOutput == null || expectedOutput.isEmpty()) {
            return false;
        }
        ItemStack effective = resolveEffectiveOutput(recipe, matrix);
        return ItemStack.areItemStacksEqual(effective, expectedOutput);
    }
}
