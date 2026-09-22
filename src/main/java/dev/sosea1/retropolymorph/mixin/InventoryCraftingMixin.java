package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingPreferenceSeeder;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(InventoryCrafting.class)
public abstract class InventoryCraftingMixin implements CraftingMatrixExtension {

    @Shadow
    @Final
    private Container eventHandler;

    @Unique
    @Nullable
    private RecipeSelectionState retropolymorph$recipeSelectionState;

    @Override
    @Nullable
    public RecipeSelectionState retropolymorph$peekRecipeSelectionState() {
        return this.retropolymorph$recipeSelectionState;
    }

    @Override
    public RecipeSelectionState retropolymorph$getOrCreateRecipeSelectionState() {
        RecipeSelectionState state = this.retropolymorph$recipeSelectionState;
        if (state == null) {
            state = new RecipeSelectionState();
            this.retropolymorph$recipeSelectionState = state;
        }
        return state;
    }

    @Override
    @Nullable
    public Container retropolymorph$getCraftingOwner() {
        return this.eventHandler;
    }

    @Inject(
            method = "setInventorySlotContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Container;onCraftMatrixChanged("
                            + "Lnet/minecraft/inventory/IInventory;)V",
                    shift = At.Shift.BEFORE),
            require = 1)
    private void retropolymorph$preseedPreferenceBeforeMatrixChange(
            int index,
            ItemStack stack,
            CallbackInfo ci) {
        CraftingPreferenceSeeder.preseed((InventoryCrafting) (Object) this, this.eventHandler);
    }
}
