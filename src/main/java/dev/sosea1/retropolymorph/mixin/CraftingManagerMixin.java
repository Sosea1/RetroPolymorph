package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProviders;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
            return;
        }

        if (matrix != null && matrix.getSizeInventory() > matrix.getWidth() * matrix.getHeight()) {
            InventoryCrafting clean = RecipeProbe.sanitizeMatrix(matrix);
            for (IRecipe recipe : ForgeRegistries.RECIPES) {
                if (recipe != null && RecipeProbe.matches(recipe, clean, world)) {
                    cir.setReturnValue(recipe);
                    return;
                }
            }
        }
    }

    @Inject(method = "findMatchingResult", at = @At("HEAD"), cancellable = true, require = 1)
    private static void retropolymorph$findMatchingResult(
            InventoryCrafting matrix,
            World world,
            CallbackInfoReturnable<ItemStack> cir) {
        InventoryCrafting clean = RecipeProbe.sanitizeMatrix(matrix);
        IRecipe selected = retropolymorph$getSelectedForOutput(matrix, world);
        if (selected != null) {
            cir.setReturnValue(RecipeProbe.craftingResult(selected, clean));
            return;
        }

        if (matrix != null && matrix.getSizeInventory() > matrix.getWidth() * matrix.getHeight()) {
            for (IRecipe recipe : ForgeRegistries.RECIPES) {
                if (recipe != null && RecipeProbe.matches(recipe, clean, world)) {
                    cir.setReturnValue(RecipeProbe.craftingResult(recipe, clean));
                    return;
                }
            }
        }
    }

    @Inject(method = "getRemainingItems", at = @At("HEAD"), cancellable = true, require = 1)
    private static void retropolymorph$getRemainingItems(
            InventoryCrafting matrix,
            World world,
            CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        InventoryCrafting clean = RecipeProbe.sanitizeMatrix(matrix);
        IRecipe selected = retropolymorph$getSelected(matrix, world);
        if (selected != null) {
            cir.setReturnValue(RecipeProbe.remainingItems(selected, clean));
            return;
        }

        if (matrix != null && matrix.getSizeInventory() > matrix.getWidth() * matrix.getHeight()) {
            for (IRecipe recipe : ForgeRegistries.RECIPES) {
                if (recipe != null && RecipeProbe.matches(recipe, clean, world)) {
                    cir.setReturnValue(RecipeProbe.remainingItems(recipe, clean));
                    return;
                }
            }
        }
    }

    @Nullable
    private static IRecipe retropolymorph$getSelectedForOutput(InventoryCrafting matrix, World world) {
        InventoryCrafting clean = RecipeProbe.sanitizeMatrix(matrix);
        RecipeSelectionState state = retropolymorph$getSelectionState(matrix);
        IRecipe selected = state == null ? null : state.resolveSelectedForOutput(clean, world);
        if (selected != null) {
            CraftingMatrixExtension extension = (CraftingMatrixExtension) matrix;
            ExternalCraftingSelectionProviders.notifyOutputResolved(
                    matrix,
                    extension.retropolymorph$getCraftingOwner(),
                    selected);
        }
        return selected;
    }

    @Nullable
    private static IRecipe retropolymorph$getSelected(InventoryCrafting matrix, World world) {
        InventoryCrafting clean = RecipeProbe.sanitizeMatrix(matrix);
        RecipeSelectionState state = retropolymorph$getSelectionState(matrix);
        return state == null ? null : state.resolveSelected(clean, world);
    }

    @Nullable
    @Unique
    private static RecipeSelectionState retropolymorph$getSelectionState(InventoryCrafting matrix) {
        CraftingMatrixExtension extension = (CraftingMatrixExtension) matrix;
        RecipeSelectionState state = extension.retropolymorph$peekRecipeSelectionState();
        Container owner = extension.retropolymorph$getCraftingOwner();

        ResourceLocation selected = ExternalCraftingSelectionProviders.getSelectedRecipeId(matrix, owner, state);

        if (selected == null) {
            if (state != null && ExternalCraftingSelectionProviders.shouldClearStateOnEmpty(matrix, owner)) {
                state.clear();
            }
            return state;
        }

        return retropolymorph$seedSelection(extension, state, selected);
    }

    @Unique
    private static RecipeSelectionState retropolymorph$seedSelection(
            CraftingMatrixExtension extension,
            @Nullable RecipeSelectionState state,
            ResourceLocation selected) {
        if (state == null) {
            state = extension.retropolymorph$getOrCreateRecipeSelectionState();
        }
        if (!selected.equals(state.getSelectedRecipeId())) {
            state.select(selected);
        }
        return state;
    }
}
