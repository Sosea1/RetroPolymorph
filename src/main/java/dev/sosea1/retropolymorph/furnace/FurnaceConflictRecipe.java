package dev.sosea1.retropolymorph.furnace;

import net.minecraft.item.ItemStack;

/** One smelting registration that vanilla rejected because its input conflicted. */
public final class FurnaceConflictRecipe {

    private final ItemStack input;
    private final ItemStack output;
    private final float experience;

    public FurnaceConflictRecipe(ItemStack input, ItemStack output, float experience) {
        this.input = input.copy();
        this.output = output.copy();
        this.experience = experience;
    }

    public ItemStack getInput() {
        return this.input;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    public float getExperience() {
        return this.experience;
    }

    public boolean sameRegistration(
            ItemStack otherInput,
            ItemStack otherOutput,
            float otherExperience) {
        return FurnaceRecipeResolver.sameInputPattern(this.input, otherInput)
                && ItemStack.areItemStacksEqual(this.output, otherOutput)
                && Float.compare(this.experience, otherExperience) == 0;
    }
}
