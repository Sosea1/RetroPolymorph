package dev.sosea1.retropolymorph.mixin.compat.extendedcrafting;

import dev.sosea1.retropolymorph.compat.extendedcrafting.ExtendedTableResultSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the matrix passed into Extended Crafting's result-slot constructor. */
@Pseudo
@Mixin(targets = "com.blakebr0.extendedcrafting.crafting.table.TableResultHandler", remap = false)
public abstract class ExtendedTableResultHandlerMixin implements ExtendedTableResultSlot {

    @Unique
    private InventoryCrafting retropolymorph$extendedCraftingMatrix;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$captureMatrix(
            InventoryCrafting crafting,
            IInventory inventory,
            World world,
            int slot,
            int x,
            int y,
            CallbackInfo callbackInfo) {
        this.retropolymorph$extendedCraftingMatrix = crafting;
    }

    @Override
    public InventoryCrafting retropolymorph$getExtendedCraftingMatrix() {
        return this.retropolymorph$extendedCraftingMatrix;
    }
}
