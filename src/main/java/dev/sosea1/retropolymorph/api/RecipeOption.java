package dev.sosea1.retropolymorph.api;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Locale;

/** Immutable presentation data for one selectable recipe result. */
public final class RecipeOption {

    private final String recipeKey;
    private final ItemStack output;
    @Nullable
    private final String policyNamespace;

    public RecipeOption(String recipeKey, ItemStack output) {
        this(recipeKey, output, null);
    }

    /**
     * @param policyNamespace optional mod namespace used by default-recipe policy
     *                        when the selection key itself is synthetic
     */
    public RecipeOption(String recipeKey, ItemStack output, @Nullable String policyNamespace) {
        this.recipeKey = recipeKey;
        this.output = output == null || output.isEmpty() ? ItemStack.EMPTY : output.copy();
        String normalized = policyNamespace == null
                ? ""
                : policyNamespace.trim().toLowerCase(Locale.ROOT);
        this.policyNamespace = normalized.isEmpty() ? null : normalized;
    }

    public String getRecipeKey() {
        return this.recipeKey;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    /**
     * Optional namespace used by mod-priority policy for synthetic recipe keys.
     * Forge-backed recipe options normally leave this unset and use the recipe ID namespace.
     */
    @Nullable
    public String getPolicyNamespace() {
        return this.policyNamespace;
    }
}
