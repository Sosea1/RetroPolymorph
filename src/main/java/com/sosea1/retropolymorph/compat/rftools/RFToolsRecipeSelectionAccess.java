package com.sosea1.retropolymorph.compat.rftools;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import javax.annotation.Nullable;

public interface RFToolsRecipeSelectionAccess {
    ItemStack retropolymorph$getOutput();
    @Nullable ResourceLocation retropolymorph$getSelectedRecipeId();
    void retropolymorph$setSelectedRecipeId(@Nullable ResourceLocation id, World world);
}
