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
 * AE2WTLib-family fallback. Some terminals implement the custom doAction in
 * ContainerWT rather than their concrete addon container.
 */
@Pseudo
@Mixin(targets = "p455w0rd.ae2wtlib.api.container.ContainerWT", remap = false)
public abstract class Ae2WtlibContainerCraftActionMixin {

    @Inject(method = "doAction", at = @At("HEAD"), remap = false, require = 0)
    private void retropolymorph$beginWirelessCraftAction(
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
    private void retropolymorph$endWirelessCraftAction(
            EntityPlayerMP player,
            InventoryAction action,
            int slot,
            long id,
            CallbackInfo ci) {
        Ae2AddonCraftActionScope.end();
    }
}
