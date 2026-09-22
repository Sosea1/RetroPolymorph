package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Treat one SlotCrafting#onTake call as an atomic selection transaction.
 *
 * <p>Vanilla 1.12 decrements crafting slots one by one and only then places
 * container/remainder items back. Without this guard, an intermediate empty
 * bucket/tool slot can invalidate the selected conflicting recipe and make a
 * shift/batch craft silently switch back to the default recipe.</p>
 */
@Mixin(SlotCrafting.class)
public abstract class SlotCraftingSelectionTransactionMixin {

    @Inject(method = "onTake", at = @At("HEAD"), require = 1)
    private void retropolymorph$beginSelectionTransaction(
            EntityPlayer player,
            ItemStack stack,
            CallbackInfoReturnable<ItemStack> cir) {
        RecipeSelectionState state = retropolymorph$getState();
        if (state != null) {
            state.beginCraftTransaction();
        }
    }

    @Inject(method = "onTake", at = @At("RETURN"), require = 1)
    private void retropolymorph$endSelectionTransaction(
            EntityPlayer player,
            ItemStack stack,
            CallbackInfoReturnable<ItemStack> cir) {
        InventoryCrafting matrix = ((SlotCraftingAccessor) this).retropolymorph$getCraftMatrix();
        RecipeSelectionState state = retropolymorph$getState(matrix);
        if (state != null) {
            state.endCraftTransaction(matrix, player.world);
        }
    }

    private RecipeSelectionState retropolymorph$getState() {
        InventoryCrafting matrix = ((SlotCraftingAccessor) this).retropolymorph$getCraftMatrix();
        return retropolymorph$getState(matrix);
    }

    private static RecipeSelectionState retropolymorph$getState(InventoryCrafting matrix) {
        if (!(matrix instanceof CraftingMatrixExtension)) {
            return null;
        }
        return ((CraftingMatrixExtension) matrix).retropolymorph$peekRecipeSelectionState();
    }
}
