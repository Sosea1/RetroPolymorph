package com.sosea1.retropolymorph.mixin.compat.rftools;

import com.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.compat.rftools.RFToolsPreviewSelection;
import com.sosea1.retropolymorph.compat.rftools.RFToolsRecipeSelectionAccess;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mcjty.rftools.blocks.crafter.CrafterBaseTE", remap = false)
public abstract class RFToolsCrafterTileMixin {
    @Unique private static final String RETROPOLYMORPH_EDITOR_TAG = "RetroPolymorphEditorRecipe";

    @Inject(method = "writeRestorableToNBT", at = @At("RETURN"), require = 1)
    private void retropolymorph$writeEditorChoice(NBTTagCompound tag, CallbackInfo ci) {
        ResourceLocation selected = RFToolsPreviewSelection.get((IInventory) this);
        if (selected == null) { tag.removeTag(RETROPOLYMORPH_EDITOR_TAG); }
        else { tag.setString(RETROPOLYMORPH_EDITOR_TAG, selected.toString()); }
    }

    @Inject(method = "readRestorableFromNBT", at = @At("RETURN"), require = 1)
    private void retropolymorph$readEditorChoice(NBTTagCompound tag, CallbackInfo ci) {
        RFToolsPreviewSelection.set((IInventory) this, RecipeKey.parseForgeId(tag.getString(RETROPOLYMORPH_EDITOR_TAG)));
    }

    @Inject(method = "selectRecipe", at = @At("RETURN"), require = 1)
    private void retropolymorph$restoreEditorChoice(int index, CallbackInfo ci) {
        try {
            Object saved = this.getClass().getMethod("getRecipe", int.class).invoke(this, index);
            if (saved instanceof RFToolsRecipeSelectionAccess) {
                RFToolsPreviewSelection.set((IInventory) this,
                        ((RFToolsRecipeSelectionAccess) saved).retropolymorph$getSelectedRecipeId());
            }
        } catch (ReflectiveOperationException exception) {
            IntegrationHealthRegistry.recordBindingFailure("rftools", "restoreEditor", exception);
        }
    }
}
