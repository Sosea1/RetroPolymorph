package dev.sosea1.retropolymorph.furnace;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selection logic for legacy FurnaceRecipes plus rejected conflict entries. */
public final class FurnaceRecipeResolver {

    private static final int WILDCARD_META = 32767;

    private FurnaceRecipeResolver() {
    }

    public static List<RecipeOption> findOptions(FurnaceRecipes recipes, ItemStack input) {
        if (input.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashMap<String, RecipeOption> options = new LinkedHashMap<String, RecipeOption>();
        Set<String> collisions = new HashSet<String>();

        for (Map.Entry<ItemStack, ItemStack> entry : recipes.getSmeltingList().entrySet()) {
            if (matches(input, entry.getKey())) {
                addOption(options, collisions, entry.getKey(), entry.getValue());
            }
        }
        for (FurnaceConflictRecipe conflict : getRejectedConflicts(recipes)) {
            if (matches(input, conflict.getInput())
                    && findLiveGuard(recipes, input, conflict.getOutput()) != null) {
                addOption(options, collisions, conflict.getInput(), conflict.getOutput());
            }
        }

        return options.size() <= 1
                ? Collections.<RecipeOption>emptyList()
                : new ArrayList<RecipeOption>(options.values());
    }

    public static boolean select(
            FurnaceRecipes recipes,
            FurnaceSelectionState state,
            ItemStack currentInput,
            String recipeKey) {
        ResolvedRecipe match = findUnique(recipes, currentInput, recipeKey);
        if (match == null) {
            return false;
        }

        match.applyTo(state, recipeKey);
        return true;
    }

    /**
     * Validates before allocating persistent per-furnace state. This is the
     * server-facing path used for untrusted/stale network selections.
     */
    public static boolean selectLazy(
            FurnaceRecipes recipes,
            IInventory furnace,
            ItemStack currentInput,
            String recipeKey) {
        ResolvedRecipe match = findUnique(recipes, currentInput, recipeKey);
        if (match == null) {
            return false;
        }

        FurnaceSelectionState state = FurnaceSelectionStore.getOrCreate(furnace);
        match.applyTo(state, recipeKey);
        return true;
    }

    public static ItemStack resolve(
            FurnaceRecipes recipes,
            ItemStack input,
            FurnaceSelectionState state) {
        String selectedKey = state.getSelectedKey();
        if (selectedKey == null || input.isEmpty()) {
            return recipes.getSmeltingResult(input);
        }

        Map<ItemStack, ItemStack> recipeMap = recipes.getSmeltingList();
        ItemStack cached = state.getCachedOutput(recipeMap, input);
        if (cached != null) {
            return cached;
        }

        ResolvedRecipe match = findUnique(recipes, input, selectedKey);
        if (match == null) {
            state.clear();
            return recipes.getSmeltingResult(input);
        }

        match.applyTo(state, selectedKey);
        return match.output;
    }

    /**
     * Returns selected recipe experience only when a concrete per-furnace
     * selection proves it. Unselected furnaces always delegate to vanilla.
     */
    public static float getSmeltingExperience(
            FurnaceRecipes recipes,
            @Nullable FurnaceSelectionState state,
            ItemStack output) {
        if (state != null && state.hasTrackedExperience(output)) {
            return state.getTrackedExperience();
        }
        return getCurrentRecipeExperience(recipes, state, output);
    }

    public static float getCurrentRecipeExperience(
            FurnaceRecipes recipes,
            @Nullable FurnaceSelectionState state,
            ItemStack output) {
        String selectedKey = state == null ? null : state.getSelectedKey();
        if (selectedKey != null) {
            Float selected = findExperienceByKey(recipes, selectedKey, output);
            if (selected != null) {
                return selected.floatValue();
            }
        }
        return recipes.getSmeltingExperience(output);
    }

    static boolean matches(ItemStack input, ItemStack recipeInput) {
        if (input.isEmpty() || recipeInput.isEmpty()) {
            return false;
        }
        return input.getItem() == recipeInput.getItem()
                && (recipeInput.getMetadata() == WILDCARD_META
                || recipeInput.getMetadata() == input.getMetadata());
    }

    static boolean sameInputPattern(ItemStack first, ItemStack second) {
        return !first.isEmpty()
                && !second.isEmpty()
                && first.getItem() == second.getItem()
                && first.getMetadata() == second.getMetadata();
    }

    private static void addOption(
            Map<String, RecipeOption> options,
            Set<String> collisions,
            ItemStack input,
            ItemStack output) {
        if (containsEquivalentOutput(options, output)) {
            return;
        }

        String key = createKey(input, output);
        if (!RecipeKey.isWireSafe(key) || collisions.contains(key)) {
            return;
        }

        RecipeOption previous = options.get(key);
        if (previous == null) {
            ResourceLocation outputId = output.getItem().getRegistryName();
            String policyNamespace = outputId == null ? null : outputId.getNamespace();
            options.put(key, new RecipeOption(key, output, policyNamespace));
            return;
        }

        if (!ItemStack.areItemStacksEqual(previous.getOutput(), output)) {
            options.remove(key);
            collisions.add(key);
        }
    }

    @Nullable
    private static ResolvedRecipe findUnique(
            FurnaceRecipes recipes,
            ItemStack input,
            String recipeKey) {
        ResolvedRecipe found = null;

        for (Map.Entry<ItemStack, ItemStack> entry : recipes.getSmeltingList().entrySet()) {
            if (!matches(input, entry.getKey())
                    || !recipeKey.equals(createKey(entry.getKey(), entry.getValue()))) {
                continue;
            }

            ResolvedRecipe candidate = ResolvedRecipe.live(entry.getKey(), entry.getValue());
            if (found != null && !found.sameOutput(candidate)) {
                return null;
            }
            found = candidate;
        }

        for (FurnaceConflictRecipe conflict : getRejectedConflicts(recipes)) {
            if (!matches(input, conflict.getInput())
                    || !recipeKey.equals(createKey(conflict.getInput(), conflict.getOutput()))) {
                continue;
            }

            Map.Entry<ItemStack, ItemStack> guard = findLiveGuard(
                    recipes, input, conflict.getOutput());
            if (guard == null) {
                continue;
            }

            ResolvedRecipe candidate = ResolvedRecipe.conflict(
                    conflict, guard.getKey(), guard.getValue());
            if (found != null && !found.sameOutput(candidate)) {
                return null;
            }
            found = candidate;
        }
        return found;
    }

    @Nullable
    private static Float findExperienceByKey(
            FurnaceRecipes recipes,
            String recipeKey,
            ItemStack output) {
        Float found = null;

        for (Map.Entry<ItemStack, ItemStack> entry : recipes.getSmeltingList().entrySet()) {
            if (!sameOutputType(output, entry.getValue())
                    || !recipeKey.equals(createKey(entry.getKey(), entry.getValue()))) {
                continue;
            }
            float experience = recipes.getSmeltingExperience(entry.getValue());
            if (found != null && Float.compare(found.floatValue(), experience) != 0) {
                return null;
            }
            found = Float.valueOf(experience);
        }

        for (FurnaceConflictRecipe conflict : getRejectedConflicts(recipes)) {
            if (!sameOutputType(output, conflict.getOutput())
                    || !recipeKey.equals(createKey(conflict.getInput(), conflict.getOutput()))) {
                continue;
            }
            float experience = conflict.getExperience();
            if (found != null && Float.compare(found.floatValue(), experience) != 0) {
                return null;
            }
            found = Float.valueOf(experience);
        }
        return found;
    }

    public static boolean registrationWasRejected(
            FurnaceRecipes recipes,
            ItemStack input,
            ItemStack output) {
        if (input.isEmpty() || output.isEmpty()) {
            return false;
        }

        if (recipes.getSmeltingList().get(input) == output) {
            return false;
        }

        ItemStack resolved = recipes.getSmeltingResult(input);
        return !resolved.isEmpty() && !ItemStack.areItemStacksEqual(resolved, output);
    }

    @Nullable
    private static Map.Entry<ItemStack, ItemStack> findLiveGuard(
            FurnaceRecipes recipes,
            ItemStack input,
            ItemStack supplementalOutput) {
        for (Map.Entry<ItemStack, ItemStack> entry : recipes.getSmeltingList().entrySet()) {
            if (matches(input, entry.getKey())
                    && !ItemStack.areItemStacksEqual(entry.getValue(), supplementalOutput)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean containsEquivalentOutput(
            Map<String, RecipeOption> options,
            ItemStack output) {
        for (RecipeOption option : options.values()) {
            if (ItemStack.areItemStacksEqual(option.getOutput(), output)) {
                return true;
            }
        }
        return false;
    }

    static boolean sameOutputType(ItemStack first, ItemStack second) {
        return !first.isEmpty()
                && !second.isEmpty()
                && first.getItem() == second.getItem()
                && first.getMetadata() == second.getMetadata()
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static List<FurnaceConflictRecipe> getRejectedConflicts(FurnaceRecipes recipes) {
        if (!(recipes instanceof FurnaceConflictExtension)) {
            return Collections.emptyList();
        }
        return ((FurnaceConflictExtension) recipes).retropolymorph$getRejectedConflicts();
    }

    @Nullable
    private static String createKey(ItemStack input, ItemStack output) {
        if (input.isEmpty() || output.isEmpty()) {
            return null;
        }

        ResourceLocation inputId = input.getItem().getRegistryName();
        ResourceLocation outputId = output.getItem().getRegistryName();
        if (inputId == null || outputId == null) {
            return null;
        }

        int nbtHash = output.hasTagCompound()
                ? output.getTagCompound().hashCode()
                : 0;
        return "smelt:"
                + inputId + '@' + input.getMetadata()
                + '>' + outputId + '@' + output.getMetadata()
                + 'x' + output.getCount()
                + '#' + Integer.toHexString(nbtHash);
    }

    private static final class ResolvedRecipe {

        private final ItemStack input;
        private final ItemStack output;

        @Nullable
        private final FurnaceConflictRecipe conflict;

        @Nullable
        private final ItemStack guardInput;

        @Nullable
        private final ItemStack guardOutput;

        private ResolvedRecipe(
                ItemStack input,
                ItemStack output,
                @Nullable FurnaceConflictRecipe conflict,
                @Nullable ItemStack guardInput,
                @Nullable ItemStack guardOutput) {
            this.input = input;
            this.output = output;
            this.conflict = conflict;
            this.guardInput = guardInput;
            this.guardOutput = guardOutput;
        }

        static ResolvedRecipe live(ItemStack input, ItemStack output) {
            return new ResolvedRecipe(input, output, null, null, null);
        }

        static ResolvedRecipe conflict(
                FurnaceConflictRecipe conflict,
                ItemStack guardInput,
                ItemStack guardOutput) {
            return new ResolvedRecipe(
                    conflict.getInput(), conflict.getOutput(), conflict, guardInput, guardOutput);
        }

        void applyTo(FurnaceSelectionState state, String recipeKey) {
            if (this.conflict == null) {
                state.selectLive(recipeKey, this.input, this.output);
            } else {
                state.selectConflict(
                        recipeKey, this.conflict, this.guardInput, this.guardOutput);
            }
        }

        boolean sameOutput(ResolvedRecipe other) {
            return ItemStack.areItemStacksEqual(this.output, other.output);
        }
    }
}
