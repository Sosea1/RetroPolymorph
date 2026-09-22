package dev.sosea1.retropolymorph.mixin.compat.thaumcraft;

import dev.sosea1.retropolymorph.compat.thaumcraft.ThaumcraftArcaneSelectionResolver;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.api.crafting.IArcaneRecipe;

/** Keeps remainders, vis spending and crystal consumption on the selected recipe. */
@Pseudo
@Mixin(targets = "thaumcraft.common.container.slot.SlotCraftingArcaneWorkbench", remap = false)
public abstract class ThaumcraftSlotCraftingArcaneWorkbenchMixin {

    @Shadow(remap = false)
    private EntityPlayer player;

    @Shadow(remap = false)
    private InventoryCrafting craftMatrix;

    @Redirect(
            method = { "func_190901_a", "onTake" },
            at = @At(
                    value = "INVOKE",
                    target = "Lthaumcraft/common/lib/crafting/ThaumcraftCraftingManager;"
                            + "findMatchingArcaneRecipe(Lnet/minecraft/inventory/InventoryCrafting;"
                            + "Lnet/minecraft/entity/player/EntityPlayer;)"
                            + "Lthaumcraft/api/crafting/IArcaneRecipe;",
                    remap = false),
            remap = false,
            require = 1)
    private IArcaneRecipe retropolymorph$selectTakenRecipe(
            InventoryCrafting matrix,
            EntityPlayer player) {
        return ThaumcraftArcaneSelectionResolver.resolve(matrix, player);
    }

    @Redirect(
            method = { "func_190901_a", "onTake" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/CraftingManager;"
                            + "func_180303_b(Lnet/minecraft/inventory/InventoryCrafting;"
                            + "Lnet/minecraft/world/World;)Lnet/minecraft/util/NonNullList;",
                    remap = false),
            remap = false,
            require = 1)
    private NonNullList<ItemStack> retropolymorph$selectedRemainders(
            InventoryCrafting matrix,
            World world) {
        if (!PolymorphConfig.isIntegrationThaumcraftEnabled()) {
            return CraftingManager.getRemainingItems(matrix, world);
        }
        IArcaneRecipe selectedArcane = ThaumcraftArcaneSelectionResolver.resolveSelected(
                this.craftMatrix, this.player);
        if (selectedArcane != null && matrix == this.craftMatrix) {
            return selectedArcane.getRemainingItems(matrix);
        }

        IRecipe selectedVanilla = ThaumcraftArcaneSelectionResolver.resolveSelectedVanilla(
                this.craftMatrix, matrix, this.player, world);
        return selectedVanilla != null
                ? selectedVanilla.getRemainingItems(matrix)
                : CraftingManager.getRemainingItems(matrix, world);
    }
}
