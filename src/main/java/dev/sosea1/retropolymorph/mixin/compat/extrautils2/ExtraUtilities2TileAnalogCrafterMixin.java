package dev.sosea1.retropolymorph.mixin.compat.extrautils2;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.compat.extrautils2.ExtraUtilities2NativeRecipeBridge;
import dev.sosea1.retropolymorph.compat.extrautils2.ExtraUtilities2SelectionAccess;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

/** Machine-owned recipe selection for XU2's Analog Crafter native recipe cache. */
@Pseudo
@Mixin(targets = "com.rwtema.extrautils2.tile.TileAnalogCrafter", remap = false)
public abstract class ExtraUtilities2TileAnalogCrafterMixin implements ExtraUtilities2SelectionAccess {

    @Unique
    private static final String RETROPOLYMORPH_RECIPE_TAG = "RetroPolymorphRecipe";

    /** XU2's getRecipe() returns this field immediately while it is non-null. */
    @Shadow(remap = false)
    @Nullable
    IRecipe curRecipe;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$extraUtilities2Recipe;

    @Override
    @Nullable
    public ResourceLocation retropolymorph$getExtraUtilities2Recipe() {
        return this.retropolymorph$extraUtilities2Recipe;
    }

    @Override
    public void retropolymorph$setExtraUtilities2Recipe(@Nullable ResourceLocation recipeId) {
        if (recipeId == null
                ? this.retropolymorph$extraUtilities2Recipe == null
                : recipeId.equals(this.retropolymorph$extraUtilities2Recipe)) {
            return;
        }
        this.retropolymorph$extraUtilities2Recipe = recipeId;

        // getRecipe() never re-scans while curRecipe is populated. Explicitly
        // invalidate the native cache whenever the selector changes/clears it.
        this.curRecipe = null;
        ((TileEntity) (Object) this).markDirty();
    }

    /**
     * Analog Crafter also bypasses CraftingManager recipe lookup: getRecipe()
     * scans CraftingHelper112.getRecipeList() directly and caches the first match.
     */
    @ModifyVariable(
            method = "getRecipe",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lcom/rwtema/extrautils2/compatibility/CraftingHelper112;"
                            + "getRecipeList()Ljava/util/List;",
                    remap = false),
            remap = false,
            require = 0)
    private List<IRecipe> retropolymorph$preferSelectedNativeRecipe(List<IRecipe> recipes) {
        if (!PolymorphConfig.isIntegrationExtraUtilities2Enabled()) {
            return recipes;
        }
        return ExtraUtilities2NativeRecipeBridge.selectedFirst(
                (TileEntity) (Object) this,
                recipes);
    }

    @Inject(
            method = {"writeToNBT", "func_189515_b"},
            at = @At("RETURN"),
            remap = false,
            require = 0)
    private void retropolymorph$writeRecipeSelection(
            NBTTagCompound tag,
            CallbackInfoReturnable<NBTTagCompound> cir) {
        if (!PolymorphConfig.isIntegrationExtraUtilities2Enabled()) {
            return;
        }
        NBTTagCompound target = cir.getReturnValue() == null ? tag : cir.getReturnValue();
        if (target == null) {
            return;
        }
        ResourceLocation selected = this.retropolymorph$extraUtilities2Recipe;
        if (selected == null) {
            target.removeTag(RETROPOLYMORPH_RECIPE_TAG);
        } else {
            target.setString(RETROPOLYMORPH_RECIPE_TAG, selected.toString());
        }
    }

    @Inject(
            method = {"readFromNBT", "func_145839_a"},
            at = @At("RETURN"),
            remap = false,
            require = 0)
    private void retropolymorph$readRecipeSelection(NBTTagCompound tag, CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationExtraUtilities2Enabled()) {
            return;
        }
        this.retropolymorph$extraUtilities2Recipe = retropolymorph$readRecipeId(tag);
        this.curRecipe = null;
    }

    @Unique
    @Nullable
    private static ResourceLocation retropolymorph$readRecipeId(@Nullable NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(RETROPOLYMORPH_RECIPE_TAG, 8)) {
            return null;
        }
        return RecipeKey.parseForgeId(tag.getString(RETROPOLYMORPH_RECIPE_TAG));
    }
}
