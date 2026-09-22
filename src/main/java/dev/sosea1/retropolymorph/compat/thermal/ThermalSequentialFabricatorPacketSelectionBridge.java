package dev.sosea1.retropolymorph.compat.thermal;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Small optional-dependency bridge between Thermal's ContainerCrafter mode
 * packet and TileCrafter#setRecipe(). The pending value is deliberately
 * transient: only a successful native Set Recipe operation may persist it.
 */
public interface ThermalSequentialFabricatorPacketSelectionBridge {

    void retropolymorph$stageRecipeSelection(@Nullable ResourceLocation recipeId);

    boolean retropolymorph$hasStagedRecipeSelection();

    @Nullable
    ResourceLocation retropolymorph$getStagedRecipeSelection();

    void retropolymorph$clearStagedRecipeSelection();
}
