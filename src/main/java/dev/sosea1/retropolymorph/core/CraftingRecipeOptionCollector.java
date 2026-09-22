package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.NonNullList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds visible crafting options while preserving registry order and collapsing only
 * recipes that are behaviorally equivalent for the current matrix.
 */
public final class CraftingRecipeOptionCollector {

    private CraftingRecipeOptionCollector() {
    }

    public static List<RecipeOption> collect(
            final RecipeSelectionContext context,
            InventoryCrafting matrix,
            List<IRecipe> matches) {
        return collect(new RecipeKeyProvider() {
            @Override
            public String getRecipeKey(IRecipe recipe) {
                return context.getRecipeKey(recipe);
            }
        }, matrix, matches);
    }

    /** Forge-registry-key variant used by focused machine surfaces. */
    public static List<RecipeOption> collectForge(
            InventoryCrafting matrix,
            List<IRecipe> matches) {
        return collect(new RecipeKeyProvider() {
            @Override
            public String getRecipeKey(IRecipe recipe) {
                ResourceLocation id = recipe == null ? null : recipe.getRegistryName();
                return id == null ? null : id.toString();
            }
        }, matrix, matches);
    }

    private static List<RecipeOption> collect(
            RecipeKeyProvider keyProvider,
            InventoryCrafting matrix,
            List<IRecipe> matches) {
        if (matches == null || matches.size() <= 1) {
            return Collections.emptyList();
        }

        ArrayList<Entry> accepted = new ArrayList<Entry>(Math.min(matches.size(), 16));
        for (IRecipe recipe : matches) {
            if (recipe == null) {
                continue;
            }

            String key = keyProvider.getRecipeKey(recipe);
            if (!RecipeKey.isWireSafe(key)) {
                continue;
            }

            ItemStack output = RecipeProbe.craftingResult(recipe, matrix);
            if (output == null || output.isEmpty()) {
                continue;
            }

            if (containsEquivalent(accepted, recipe, output, matrix)) {
                continue;
            }
            accepted.add(new Entry(recipe, new RecipeOption(key, output.copy())));
        }

        if (accepted.size() <= 1) {
            return Collections.emptyList();
        }

        ArrayList<RecipeOption> result = new ArrayList<RecipeOption>(accepted.size());
        for (Entry entry : accepted) {
            result.add(entry.option);
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean containsEquivalent(
            List<Entry> accepted,
            IRecipe recipe,
            ItemStack output,
            InventoryCrafting matrix) {
        for (Entry existing : accepted) {
            if (!ItemStack.areItemStacksEqual(existing.option.getOutput(), output)) {
                continue;
            }

            if (sameRemainders(existing.recipe, recipe, matrix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * If either custom recipe cannot safely expose remainders during the probe, keep both entries.
     * Deduplication is an optimization/UX cleanup and must never become a correctness requirement.
     */
    private static boolean sameRemainders(
            IRecipe first,
            IRecipe second,
            InventoryCrafting matrix) {
        NonNullList<ItemStack> a;
        NonNullList<ItemStack> b;
        try {
            a = first.getRemainingItems(matrix);
        } catch (RuntimeException | LinkageError exception) {
            RecipeProbe.reportRemainderFailure(first, exception);
            return false;
        }
        try {
            b = second.getRemainingItems(matrix);
        } catch (RuntimeException | LinkageError exception) {
            RecipeProbe.reportRemainderFailure(second, exception);
            return false;
        }

        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }

        for (int slot = 0; slot < a.size(); slot++) {
            ItemStack left = a.get(slot);
            ItemStack right = b.get(slot);
            if (!ItemStack.areItemStacksEqual(
                    left == null ? ItemStack.EMPTY : left,
                    right == null ? ItemStack.EMPTY : right)) {
                return false;
            }
        }
        return true;
    }

    private interface RecipeKeyProvider {
        String getRecipeKey(IRecipe recipe);
    }

    private static final class Entry {
        private final IRecipe recipe;
        private final RecipeOption option;

        private Entry(IRecipe recipe, RecipeOption option) {
            this.recipe = recipe;
            this.option = option;
        }
    }
}
