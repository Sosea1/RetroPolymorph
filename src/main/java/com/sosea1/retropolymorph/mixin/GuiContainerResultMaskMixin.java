package com.sosea1.retropolymorph.mixin;

import com.sosea1.retropolymorph.client.ClientGuiEvents;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Never show or click a transient native result before the current server reply. */
@Mixin(GuiContainer.class)
public abstract class GuiContainerResultMaskMixin {

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void retropolymorph$maskPendingSlot(Slot slot, CallbackInfo ci) {
        if (ClientGuiEvents.shouldMaskResult((GuiContainer) (Object) this, slot)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHoveredToolTip", at = @At("HEAD"), cancellable = true)
    private void retropolymorph$maskPendingTooltip(int mouseX, int mouseY, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        if (ClientGuiEvents.shouldMaskResult(gui, gui.getSlotUnderMouse())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void retropolymorph$maskPendingClick(
            Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (ClientGuiEvents.shouldMaskResult((GuiContainer) (Object) this, slot)) {
            ci.cancel();
        }
    }
}
