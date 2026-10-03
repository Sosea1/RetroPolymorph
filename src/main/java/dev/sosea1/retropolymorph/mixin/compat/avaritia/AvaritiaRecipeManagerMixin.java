package dev.sosea1.retropolymorph.mixin.compat.avaritia;

import dev.sosea1.retropolymorph.compat.avaritia.AvaritiaExtremeCraftingReflection;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "morph.avaritia.recipe.AvaritiaRecipeManager", remap = false)
public abstract class AvaritiaRecipeManagerMixin {

    @Inject(
            method = "getExtremeCraftingResult",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private static void retropolymorph$selectedResult(
            InventoryCrafting matrix,
            World world,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!PolymorphConfig.isIntegrationAvaritiaEnabled()
                || !AvaritiaExtremeCraftingReflection.hasSelection(matrix)) {
            return;
        }
        ItemStack result = AvaritiaExtremeCraftingReflection.selectedCraftingResult(matrix, world);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(
            method = "getRemainingItems",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private static void retropolymorph$selectedRemainingItems(
            InventoryCrafting matrix,
            World world,
            CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        if (!PolymorphConfig.isIntegrationAvaritiaEnabled()
                || !AvaritiaExtremeCraftingReflection.hasSelection(matrix)) {
            return;
        }
        NonNullList<ItemStack> remaining =
                AvaritiaExtremeCraftingReflection.selectedRemainingItems(matrix, world);
        if (remaining != null) {
            cir.setReturnValue(remaining);
        }
    }
}
