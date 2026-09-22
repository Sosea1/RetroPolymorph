package dev.sosea1.retropolymorph.mixin.compat.tconstruct;

import dev.sosea1.retropolymorph.compat.tconstruct.TinkersCraftingStationAccess;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidates Tinkers' Crafting Station FastWorkbench-style cached recipe
 * when a RetroPolymorph recipe selection is present or changed.
 */
@Pseudo
@Mixin(targets = "slimeknights.tconstruct.tools.common.inventory.ContainerCraftingStation", remap = false)
public abstract class TinkersCraftingStationMixin implements TinkersCraftingStationAccess {

    @Shadow(remap = false)
    private IRecipe lastRecipe;

    @Shadow(remap = false)
    private IRecipe lastLastRecipe;

    @Override
    public void retropolymorph$clearLastRecipe() {
        this.lastRecipe = null;
        this.lastLastRecipe = null;
    }

    @Inject(
            method = {"func_192389_a", "slotChangedCraftingGrid"},
            at = @At("HEAD"),
            remap = false,
            require = 0)
    private void retropolymorph$invalidateCachedRecipe(
            World world,
            EntityPlayer player,
            InventoryCrafting matrix,
            InventoryCraftResult result,
            CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationTinkersEnabled()) {
            return;
        }

        if (matrix instanceof CraftingMatrixExtension) {
            CraftingMatrixExtension ext = (CraftingMatrixExtension) matrix;
            RecipeSelectionState state = ext.retropolymorph$peekRecipeSelectionState();
            if (state != null && state.hasSelection()) {
                ResourceLocation selectedId = state.getSelectedRecipeId();
                if (selectedId != null) {
                    if (this.lastRecipe == null || !selectedId.equals(this.lastRecipe.getRegistryName())) {
                        this.lastRecipe = null;
                        this.lastLastRecipe = null;
                    }
                }
            }
        }
    }
}
