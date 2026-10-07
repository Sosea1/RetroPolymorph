package com.sosea1.retropolymorph.mixin.compat.rftools;

import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.compat.rftools.RFToolsPreviewSelection;
import com.sosea1.retropolymorph.compat.rftools.RFToolsRecipeSelectionAccess;
import com.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import javax.annotation.Nullable;

@Pseudo
@Mixin(targets = "mcjty.rftools.craftinggrid.CraftingRecipe", remap = false)
public abstract class RFToolsCraftingRecipeMixin implements RFToolsRecipeSelectionAccess {
    @Shadow private InventoryCrafting inv;
    @Shadow private ItemStack result;
    @Shadow private boolean recipePresent;
    @Shadow private IRecipe recipe;
    @Shadow public abstract IRecipe getCachedRecipe(World world);
    @Unique private ResourceLocation retropolymorph$selected;
    @Unique private boolean retropolymorph$refreshResultAfterFallback;
    @Unique private static final String RETROPOLYMORPH_TAG = "RetroPolymorphRecipe";

    public ItemStack retropolymorph$getOutput() { return this.result; }
    @Nullable public ResourceLocation retropolymorph$getSelectedRecipeId() { return this.retropolymorph$selected; }

    public void retropolymorph$setSelectedRecipeId(@Nullable ResourceLocation id, World world) {
        IRecipe selected = RFToolsPreviewSelection.resolve(id, this.inv, world);
        this.retropolymorph$selected = selected == null ? null : id;
        this.recipe = selected;
        this.recipePresent = selected != null;
        IRecipe resolved = selected != null ? selected : getCachedRecipe(world);
        this.result = resolved == null ? ItemStack.EMPTY : RecipeProbe.craftingResult(resolved, this.inv);
    }

    @Inject(method = "getCachedRecipe", at = @At("HEAD"), cancellable = true, require = 1)
    private void retropolymorph$resolveSelected(World world, CallbackInfoReturnable<IRecipe> cir) {
        this.retropolymorph$refreshResultAfterFallback = false;
        if (this.retropolymorph$selected == null) { return; }
        IRecipe selected = RFToolsPreviewSelection.resolve(this.retropolymorph$selected, this.inv, world);
        if (selected == null) {
            this.retropolymorph$selected = null;
            this.recipe = null;
            this.recipePresent = false;
            this.retropolymorph$refreshResultAfterFallback = true;
            return;
        }
        this.recipe = selected;
        this.recipePresent = true;
        cir.setReturnValue(selected);
    }

    @Inject(method = "getCachedRecipe", at = @At("RETURN"), require = 1)
    private void retropolymorph$refreshFallbackResult(World world, CallbackInfoReturnable<IRecipe> cir) {
        if (!this.retropolymorph$refreshResultAfterFallback) { return; }
        this.retropolymorph$refreshResultAfterFallback = false;
        IRecipe resolved = cir.getReturnValue();
        this.result = resolved == null ? ItemStack.EMPTY : RecipeProbe.craftingResult(resolved, this.inv);
    }

    @Inject(method = "setRecipe", at = @At("HEAD"), require = 1)
    private void retropolymorph$replaceTemplate(CallbackInfo ci) {
        this.retropolymorph$selected = null;
        this.recipe = null;
        this.recipePresent = false;
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"), require = 1)
    private void retropolymorph$readSelection(NBTTagCompound tag, CallbackInfo ci) {
        this.retropolymorph$selected = RecipeKey.parseForgeId(tag.getString(RETROPOLYMORPH_TAG));
        this.recipe = null;
        this.recipePresent = false;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"), require = 1)
    private void retropolymorph$writeSelection(NBTTagCompound tag, CallbackInfo ci) {
        if (this.retropolymorph$selected == null) { tag.removeTag(RETROPOLYMORPH_TAG); }
        else { tag.setString(RETROPOLYMORPH_TAG, this.retropolymorph$selected.toString()); }
    }
}
