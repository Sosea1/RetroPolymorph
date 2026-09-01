package dev.sosea1.retropolymorph.furnace;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Tiny per-furnace selection state.
 *
 * The persistent key survives world reloads. Runtime references include one
 * live FurnaceRecipes map entry used as an O(1) validity guard, so rejected
 * supplemental recipes cannot outlive the live conflict that made them useful.
 */
public final class FurnaceSelectionState {

    @Nullable
    private String selectedKey;

    @Nullable
    private ItemStack cachedLiveInputKey;

    @Nullable
    private ItemStack cachedLiveOutput;

    @Nullable
    private ItemStack cachedSelectedOutput;

    @Nullable
    private FurnaceConflictRecipe cachedConflict;

    @Nullable
    private ItemStack trackedOutput;

    private float trackedExperience;

    @Nullable
    public String getSelectedKey() {
        return this.selectedKey;
    }

    public boolean hasSelection() {
        return this.selectedKey != null;
    }

    public void selectLive(String recipeKey, ItemStack inputKey, ItemStack output) {
        this.selectedKey = recipeKey;
        this.cachedLiveInputKey = inputKey;
        this.cachedLiveOutput = output;
        this.cachedSelectedOutput = output;
        this.cachedConflict = null;
    }

    public void selectConflict(
            String recipeKey,
            FurnaceConflictRecipe conflict,
            ItemStack guardInputKey,
            ItemStack guardOutput) {
        this.selectedKey = recipeKey;
        this.cachedLiveInputKey = guardInputKey;
        this.cachedLiveOutput = guardOutput;
        this.cachedSelectedOutput = conflict.getOutput();
        this.cachedConflict = conflict;
    }

    public void setPersistentKey(@Nullable String recipeKey) {
        this.selectedKey = recipeKey;
        invalidateCache();
    }

    public void clear() {
        this.selectedKey = null;
        invalidateCache();
    }

    @Nullable
    ItemStack getCachedOutput(Map<ItemStack, ItemStack> recipes, ItemStack currentInput) {
        if (this.cachedLiveInputKey == null
                || this.cachedLiveOutput == null
                || this.cachedSelectedOutput == null) {
            return null;
        }

        ItemStack mapped = recipes.get(this.cachedLiveInputKey);
        if (mapped != this.cachedLiveOutput
                || !FurnaceRecipeResolver.matches(currentInput, this.cachedLiveInputKey)) {
            invalidateCache();
            return null;
        }

        if (this.cachedConflict != null
                && !FurnaceRecipeResolver.matches(currentInput, this.cachedConflict.getInput())) {
            invalidateCache();
            return null;
        }
        return this.cachedSelectedOutput;
    }

    public void recordProducedOutput(
            ItemStack existingOutput,
            ItemStack producedOutput,
            float producedExperience,
            float vanillaExistingExperience) {
        if (producedOutput.isEmpty()) {
            return;
        }

        int existingCount = existingOutput.isEmpty() ? 0 : existingOutput.getCount();
        int producedCount = producedOutput.getCount();
        if (producedCount <= 0) {
            return;
        }

        float existingExperience = vanillaExistingExperience;
        if (existingCount > 0 && hasTrackedExperience(existingOutput)) {
            existingExperience = this.trackedExperience;
        }

        int combinedCount = existingCount + producedCount;
        this.trackedExperience = (existingExperience * existingCount
                + producedExperience * producedCount) / combinedCount;
        this.trackedOutput = (existingCount > 0 ? existingOutput : producedOutput).copy();
    }

    public boolean hasTrackedExperience(ItemStack output) {
        return this.trackedOutput != null
                && FurnaceRecipeResolver.sameOutputType(this.trackedOutput, output);
    }

    public float getTrackedExperience() {
        return this.trackedExperience;
    }

    public void restoreTrackedExperience(ItemStack output, float experience) {
        if (output.isEmpty()) {
            clearTrackedExperience();
            return;
        }
        this.trackedOutput = output.copy();
        this.trackedExperience = experience;
    }

    public void clearTrackedExperience() {
        this.trackedOutput = null;
        this.trackedExperience = 0.0F;
    }

    private void invalidateCache() {
        this.cachedLiveInputKey = null;
        this.cachedLiveOutput = null;
        this.cachedSelectedOutput = null;
        this.cachedConflict = null;
    }
}
