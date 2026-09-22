package dev.sosea1.retropolymorph.mixin.compat.rftoolscontrol;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.compat.rftools.RFToolsWorkbenchSelectionExtension;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/** Seeds a selected recipe into every temporary matrix made by the Workbench. */
@Pseudo
@Mixin(targets = "mcjty.rftoolscontrol.blocks.workbench.WorkbenchTileEntity", remap = false)
public abstract class RFToolsWorkbenchMixin implements RFToolsWorkbenchSelectionExtension {

    @Unique
    private static final String RETROPOLYMORPH_RECIPE_TAG =
            "retroPolymorphSelectedWorkbenchRecipe";

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$workbenchRecipeId;

    @Unique
    @Nullable
    private InventoryCrafting retropolymorph$previewMatrix;

    @Unique
    private boolean retropolymorph$resolvingPreview;

    @Unique
    private boolean retropolymorph$selectionObserved;

    @Shadow(remap = false)
    private void updateRecipe() {
    }

    @Override
    @Nullable
    public ResourceLocation retropolymorph$getWorkbenchRecipeId() {
        return this.retropolymorph$workbenchRecipeId;
    }

    @Override
    public void retropolymorph$setWorkbenchRecipeId(@Nullable ResourceLocation recipeId) {
        this.retropolymorph$workbenchRecipeId = recipeId;
        ((TileEntity) (Object) this).markDirty();
    }

    @Override
    public void retropolymorph$refreshWorkbenchRecipe() {
        updateRecipe();
    }

    @Override
    public boolean retropolymorph$wasWorkbenchSelectionObserved() {
        return this.retropolymorph$selectionObserved;
    }

    @Inject(method = "updateRecipe", at = @At("HEAD"), remap = false, require = 1)
    private void retropolymorph$beginRecipeUpdate(CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationRftoolsEnabled()) {
            return;
        }
        this.retropolymorph$resolvingPreview = true;
        this.retropolymorph$previewMatrix = null;
        this.retropolymorph$selectionObserved = false;
    }

    @Inject(method = "makeWorkInventory", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$seedTemporaryMatrix(
            CallbackInfoReturnable<InventoryCrafting> cir) {
        if (!PolymorphConfig.isIntegrationRftoolsEnabled()) {
            return;
        }
        InventoryCrafting matrix = cir.getReturnValue();
        RecipeSelectionSeeder.seed(matrix, this.retropolymorph$workbenchRecipeId);
        if (this.retropolymorph$resolvingPreview) {
            this.retropolymorph$previewMatrix = matrix;
        }
    }

    @Inject(method = "updateRecipe", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$finishRecipeUpdate(CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationRftoolsEnabled()) {
            return;
        }
        ResourceLocation selected = this.retropolymorph$workbenchRecipeId;
        InventoryCrafting matrix = this.retropolymorph$previewMatrix;
        this.retropolymorph$selectionObserved = matrix != null
                && RecipeSelectionSeeder.wasOutputResolutionObserved(matrix, selected);
        this.retropolymorph$resolvingPreview = false;
        this.retropolymorph$previewMatrix = null;

        if (selected != null && !this.retropolymorph$selectionObserved) {
            this.retropolymorph$workbenchRecipeId = null;
            ((TileEntity) (Object) this).markDirty();
        }
    }

    @Inject(method = "readRestorableFromNBT", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$readSelection(NBTTagCompound compound, CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationRftoolsEnabled()) {
            return;
        }
        this.retropolymorph$workbenchRecipeId =
                RecipeKey.parseForgeId(compound.getString(RETROPOLYMORPH_RECIPE_TAG));
    }

    @Inject(method = "writeRestorableToNBT", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$writeSelection(NBTTagCompound compound, CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationRftoolsEnabled()) {
            return;
        }
        ResourceLocation selected = this.retropolymorph$workbenchRecipeId;
        if (selected == null) {
            compound.removeTag(RETROPOLYMORPH_RECIPE_TAG);
        } else {
            compound.setString(RETROPOLYMORPH_RECIPE_TAG, selected.toString());
        }
    }
}
