package dev.sosea1.retropolymorph.api;

import net.minecraft.item.ItemStack;

/** Immutable presentation data for one selectable recipe result. */
public final class RecipeOption {

    private final String recipeKey;
    private final ItemStack output;

    public RecipeOption(String recipeKey, ItemStack output) {
        this.recipeKey = recipeKey;
        this.output = output == null || output.isEmpty() ? ItemStack.EMPTY : output.copy();
    }

    public String getRecipeKey() {
        return this.recipeKey;
    }

    public ItemStack getOutput() {
        return this.output;
    }
}
