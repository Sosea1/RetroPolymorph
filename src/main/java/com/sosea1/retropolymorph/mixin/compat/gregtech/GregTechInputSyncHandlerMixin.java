package com.sosea1.retropolymorph.mixin.compat.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "gregtech.common.mui.widget.workbench.CraftingInputSlot$InputSyncHandler", remap = false)
public abstract class GregTechInputSyncHandlerMixin {

    @Shadow(remap = false)
    private ItemStack lastStoredItem;

    @Shadow(remap = false)
    public abstract ItemStack getStack();

    @Inject(method = "readOnClient", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$acknowledgeReceivedSlot(int id, PacketBuffer buffer, CallbackInfo ci) {
        if (id != 1) {
            return;
        }
        // Only a received client packet is acknowledged here. The server must
        // keep its old snapshot until native detectAndSendChanges sends the edit.
        ItemStack stack = getStack();
        this.lastStoredItem = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}
