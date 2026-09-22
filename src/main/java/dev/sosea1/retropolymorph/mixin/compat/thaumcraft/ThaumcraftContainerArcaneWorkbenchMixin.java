package dev.sosea1.retropolymorph.mixin.compat.thaumcraft;

import dev.sosea1.retropolymorph.compat.thaumcraft.ThaumcraftArcaneLifecycle;
import dev.sosea1.retropolymorph.compat.thaumcraft.ThaumcraftArcaneSelectionResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.api.crafting.IArcaneRecipe;

/** Routes Arcane Workbench preview through the selected arcane or vanilla engine. */
@Pseudo
@Mixin(targets = "thaumcraft.common.container.ContainerArcaneWorkbench", remap = false)
public abstract class ThaumcraftContainerArcaneWorkbenchMixin {

    private static final String ARCANE_LOOKUP =
            "Lthaumcraft/common/lib/crafting/ThaumcraftCraftingManager;"
                    + "findMatchingArcaneRecipe(Lnet/minecraft/inventory/InventoryCrafting;"
                    + "Lnet/minecraft/entity/player/EntityPlayer;)"
                    + "Lthaumcraft/api/crafting/IArcaneRecipe;";

    @Inject(method = { "func_75134_a", "onContainerClosed" }, at = @At("HEAD"), remap = false, require = 1)
    private void retropolymorph$clearSelectionOnClose(
            EntityPlayer player,
            CallbackInfo ci) {
        if (!dev.sosea1.retropolymorph.config.PolymorphConfig.isIntegrationThaumcraftEnabled()) {
            return;
        }
        ThaumcraftArcaneLifecycle.clearContainerSelection((Container) (Object) this, player);
    }

    @Redirect(
            method = { "func_75130_a", "onCraftMatrixChanged" },
            at = @At(value = "INVOKE", target = ARCANE_LOOKUP, remap = false),
            remap = false,
            require = 1)
    private IArcaneRecipe retropolymorph$selectMatrixChangedRecipe(
            InventoryCrafting matrix,
            EntityPlayer player) {
        return ThaumcraftArcaneSelectionResolver.resolve(matrix, player);
    }

    @Redirect(
            method = { "func_192389_a", "slotChangedCraftingGrid" },
            at = @At(value = "INVOKE", target = ARCANE_LOOKUP, remap = false),
            remap = false,
            require = 1)
    private IArcaneRecipe retropolymorph$selectCraftResultRecipe(
            InventoryCrafting matrix,
            EntityPlayer player) {
        return ThaumcraftArcaneSelectionResolver.resolve(matrix, player);
    }
    @Redirect(
            method = { "func_192389_a", "slotChangedCraftingGrid" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/CraftingManager;"
                            + "func_192413_b(Lnet/minecraft/inventory/InventoryCrafting;"
                            + "Lnet/minecraft/world/World;)Lnet/minecraft/item/crafting/IRecipe;",
                    remap = false),
            remap = false,
            require = 1)
    private IRecipe retropolymorph$selectVanillaFallbackRecipe(
            InventoryCrafting vanillaMatrix,
            World world,
            World methodWorld,
            EntityPlayer player,
            InventoryCrafting arcaneMatrix,
            InventoryCraftResult craftResult) {
        return ThaumcraftArcaneSelectionResolver.resolveVanillaFallback(
                arcaneMatrix, vanillaMatrix, player, world);
    }

}
