package com.sosea1.retropolymorph.mixin.compat.gregtech;

import com.sosea1.retropolymorph.compat.gregtech.GregTechWorkbenchMemory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "gregtech.common.metatileentities.storage.CraftingRecipeMemory", remap = false)
public abstract class GregTechCraftingRecipeMemoryMixin {

    @Inject(method = "loadRecipe", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$onRecipeLoaded(int index, CallbackInfo ci) {
        GregTechWorkbenchMemory.onRecipeLoadedCeu(this, index);
    }
}
