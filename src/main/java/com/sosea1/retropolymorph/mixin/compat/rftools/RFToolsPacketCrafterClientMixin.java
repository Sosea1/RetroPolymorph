package com.sosea1.retropolymorph.mixin.compat.rftools;

import com.sosea1.retropolymorph.compat.rftools.RFToolsPacketSelectionAccess;
import com.sosea1.retropolymorph.compat.rftools.RFToolsPreviewSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mcjty.rftools.blocks.crafter.PacketCrafter", remap = false)
public abstract class RFToolsPacketCrafterClientMixin {
    @Inject(method = "<init>(Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/item/ItemStack;ZLmcjty/rftools/craftinggrid/CraftingRecipe$CraftMode;)V",
            at = @At("RETURN"), require = 1)
    private void retropolymorph$captureEditorChoice(CallbackInfo ci) {
        if (Minecraft.getMinecraft().player == null) { return; }
        Container container = Minecraft.getMinecraft().player.openContainer;
        if (container == null || !container.getClass().getName().equals("mcjty.rftools.blocks.crafter.CrafterContainer")
                || container.inventorySlots.size() < 10) { return; }
        ((RFToolsPacketSelectionAccess) this).retropolymorph$setRecipeId(
                RFToolsPreviewSelection.get(container.getSlot(0).inventory));
    }
}
