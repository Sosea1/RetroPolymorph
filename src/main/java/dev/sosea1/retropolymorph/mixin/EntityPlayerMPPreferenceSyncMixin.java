package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.core.CraftingPreferenceSeeder;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores a remembered player selection immediately before the server sends
 * the open container's slot state back to that player.
 *
 * <p>This is intentionally above individual container implementations: virtual
 * dispatch still invokes the modded container's own detectAndSendChanges(), so
 * AE2/WCT and other dedicated crafting GUIs participate without per-mod player
 * lookup hooks.</p>
 */
@Mixin(EntityPlayerMP.class)
public abstract class EntityPlayerMPPreferenceSyncMixin {

    @Inject(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Container;detectAndSendChanges()V",
                    shift = At.Shift.BEFORE),
            require = 1)
    private void retropolymorph$restorePreferenceBeforeContainerSync(CallbackInfo ci) {
        EntityPlayerMP player = (EntityPlayerMP) (Object) this;
        CraftingPreferenceSeeder.preseed(player, player.openContainer);
    }
}
