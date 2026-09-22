package dev.sosea1.retropolymorph.mixin.compat.ae2;

import appeng.helpers.InventoryAction;
import dev.sosea1.retropolymorph.compat.ae2.Ae2AddonCraftActionScope;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WCT 3.12.x has its own SlotCraftingOutput and custom PacketInventoryAction,
 * so native AE2 SlotCraftingTerm#doClick is never reached. Bracket WCT's
 * doAction directly instead.
 */
@Pseudo
@Mixin(targets = "p455w0rd.wct.container.ContainerWCT", remap = false)
public abstract class WctContainerCraftActionMixin {

    @Inject(method = "doAction", at = @At("HEAD"), remap = false, require = 0)
    private void retropolymorph$beginWctCraftAction(
            EntityPlayerMP player,
            InventoryAction action,
            int slot,
            long id,
            CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationAe2Enabled()) {
            return;
        }
        Ae2AddonCraftActionScope.begin((Container) (Object) this, player, action, slot);
    }

    @Inject(method = "doAction", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$endWctCraftAction(
            EntityPlayerMP player,
            InventoryAction action,
            int slot,
            long id,
            CallbackInfo ci) {
        Ae2AddonCraftActionScope.end();
    }
}
