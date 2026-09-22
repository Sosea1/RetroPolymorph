package dev.sosea1.retropolymorph.compat.extendedcrafting;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public interface EnderCrafterSelectionAccess {

    int retropolymorph$getEnderCrafterRecipeIndex();

    @Nullable
    ItemStack retropolymorph$getExpectedOutput();

    void retropolymorph$setEnderCrafterRecipe(int index, @Nullable ItemStack expectedOutput);
}
