package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(CraftingManager.class)
public abstract class CraftingManagerMixin {

    @Inject(method = "findMatchingRecipe", at = @At("HEAD"), cancellable = true, require = 1)
    private static void retropolymorph$findMatchingRecipe(
            InventoryCrafting matrix,
            World world,
            CallbackInfoReturnable<IRecipe> cir) {
        IRecipe selected = retropolymorph$getSelectedForOutput(matrix, world);
        if (selected != null) {
            cir.setReturnValue(selected);
        }
    }

    @Inject(method = "findMatchingResult", at = @At("HEAD"), cancellable = true, require = 1)
    private static void retropolymorph$findMatchingResult(
            InventoryCrafting matrix,
            World world,
            CallbackInfoReturnable<ItemStack> cir) {
        IRecipe selected = retropolymorph$getSelectedForOutput(matrix, world);
        if (selected != null) {
            cir.setReturnValue(selected.getCraftingResult(matrix));
        }
    }

    @Inject(method = "getRemainingItems", at = @At("HEAD"), cancellable = true, require = 1)
    private static void retropolymorph$getRemainingItems(
            InventoryCrafting matrix,
            World world,
            CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        IRecipe selected = retropolymorph$getSelected(matrix, world);
        if (selected != null) {
            cir.setReturnValue(selected.getRemainingItems(matrix));
        }
    }

    @Nullable
    private static IRecipe retropolymorph$getSelectedForOutput(InventoryCrafting matrix, World world) {
        RecipeSelectionState state =
                ((CraftingMatrixExtension) matrix).retropolymorph$peekRecipeSelectionState();
        return state == null ? null : state.resolveSelectedForOutput(matrix, world);
    }

    @Nullable
    private static IRecipe retropolymorph$getSelected(InventoryCrafting matrix, World world) {
        RecipeSelectionState state =
                ((CraftingMatrixExtension) matrix).retropolymorph$peekRecipeSelectionState();
        return state == null ? null : state.resolveSelected(matrix, world);
    }
}
