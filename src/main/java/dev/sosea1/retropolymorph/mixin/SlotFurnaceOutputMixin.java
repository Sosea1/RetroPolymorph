package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.furnace.FurnaceRecipeResolver;
import dev.sosea1.retropolymorph.furnace.FurnaceSelectionStore;
import dev.sosea1.retropolymorph.furnace.FurnaceSelectionState;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.SlotFurnaceOutput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserves experience for selectable smelting recipes rejected by vanilla. */
@Mixin(SlotFurnaceOutput.class)
public abstract class SlotFurnaceOutputMixin {

    @Redirect(
            method = "onCrafting(Lnet/minecraft/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/FurnaceRecipes;"
                            + "getSmeltingExperience(Lnet/minecraft/item/ItemStack;)F"),
            require = 1)
    private float retropolymorph$getSelectedExperience(FurnaceRecipes recipes, ItemStack output) {
        IInventory inventory = ((SlotInventoryAccessor) (Object) this).retropolymorph$getInventory();
        FurnaceSelectionState state = FurnaceSelectionStore.peek(inventory);
        return FurnaceRecipeResolver.getSmeltingExperience(recipes, state, output);
    }
}
