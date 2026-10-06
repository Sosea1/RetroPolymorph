package com.sosea1.retropolymorph.mixin.compat.artisanworktables;

import com.sosea1.retropolymorph.compat.artisanworktables.ArtisanWorktableReflection;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "com.codetaylor.mc.artisanworktables.modules.worktables.tile.spi.TileEntityBase",
        remap = false)
public abstract class ArtisanTileRecipeMixin {

    @Inject(method = "getRecipe", at = @At("HEAD"), remap = false, require = 0)
    private void retropolymorph$beginSelectedRecipe(
            EntityPlayer player,
            CallbackInfoReturnable<Object> cir) {
        if (PolymorphConfig.isIntegrationArtisanWorktablesEnabled()) {
            ArtisanWorktableReflection.beginSelectedRecipe(this, player);
        }
    }

    @Inject(method = "getRecipe", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$finishSelectedRecipe(
            EntityPlayer player,
            CallbackInfoReturnable<Object> cir) {
        if (PolymorphConfig.isIntegrationArtisanWorktablesEnabled()) {
            ArtisanWorktableReflection.finishSelectedRecipe(this, player, cir.getReturnValue());
        }
    }
}
