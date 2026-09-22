package dev.sosea1.retropolymorph.mixin.compat.cyclic;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.compat.cyclic.CyclicSelectionAccess;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.InventoryCrafting;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Pseudo
@Mixin(targets = "com.lothrazar.cyclicmagic.block.crafter.TileEntityCrafter", remap = false)
public abstract class CyclicTileCrafterMixin implements CyclicSelectionAccess {

    @Unique
    private static final String RETROPOLYMORPH_RECIPE_TAG = "RetroPolymorphRecipe";

    @Shadow(remap = false)
    private IRecipe recipe;

    @Shadow(remap = false)
    private InventoryCrafting crafter;

    @Shadow(remap = false)
    private int lastInvHash;

    @Shadow(remap = false)
    protected abstract void findRecipe();

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$selectedRecipeId;

    @Override
    @Nullable
    public ResourceLocation retropolymorph$getSelectedRecipeId() {
        return this.retropolymorph$selectedRecipeId;
    }

    @Override
    public void retropolymorph$setSelectedRecipeId(@Nullable ResourceLocation recipeId) {
        this.retropolymorph$selectedRecipeId = recipeId;
        this.recipe = null;
        this.lastInvHash = -1;
        try {
            findRecipe();
        } catch (RuntimeException | LinkageError t) {
            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                    "cyclic",
                    "findRecipe",
                    t);
            org.apache.logging.log4j.LogManager.getLogger("Retro Polymorph")
                    .warn("Failed to invoke findRecipe on Cyclic TileEntityCrafter", t);
        }
        if (((Object) this) instanceof TileEntity) {
            ((TileEntity) (Object) this).markDirty();
        }
    }

    @Inject(method = "findRecipe", at = @At("HEAD"), remap = false, require = 0)
    private void retropolymorph$seedCrafterRecipe(CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationCyclicEnabled()) {
            return;
        }
        if (this.crafter != null) {
            this.recipe = null;
            if (this.retropolymorph$selectedRecipeId != null) {
                RecipeSelectionSeeder.seed(this.crafter, this.retropolymorph$selectedRecipeId);
            }
        }
    }

    @Inject(
            method = {"writeToNBT", "func_189515_b"},
            at = @At("RETURN"),
            remap = false,
            require = 0)
    private void retropolymorph$writeRecipeSelection(
            NBTTagCompound tag,
            CallbackInfoReturnable<NBTTagCompound> cir) {
        if (!PolymorphConfig.isIntegrationCyclicEnabled()) {
            return;
        }
        NBTTagCompound target = cir.getReturnValue() == null ? tag : cir.getReturnValue();
        if (target == null) {
            return;
        }
        ResourceLocation selected = this.retropolymorph$selectedRecipeId;
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
        if (!PolymorphConfig.isIntegrationCyclicEnabled()) {
            return;
        }
        if (tag != null && tag.hasKey(RETROPOLYMORPH_RECIPE_TAG, 8)) {
            this.retropolymorph$selectedRecipeId =
                    RecipeKey.parseForgeId(tag.getString(RETROPOLYMORPH_RECIPE_TAG));
            this.recipe = null;
        }
    }
}
