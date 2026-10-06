package com.sosea1.retropolymorph.mixin.compat.gregtech;

import com.sosea1.retropolymorph.compat.gregtech.GregTechWorkbenchMemory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "gregtech.common.gui.widget.craftingstation.MemorizedRecipeWidget",
        "gregtech.common.gui.widget.MemorizedRecipeWidget"
}, remap = false)
public abstract class GregTechRecipeMemoryWidgetMixin {
    @Inject(method = "slotClick", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$restoreMemorizedOutput(int dragType, ClickType clickType,
            EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        if (clickType == ClickType.PICKUP) {
            GregTechWorkbenchMemory.onRecipeLoaded(this, player);
        }
    }
}
