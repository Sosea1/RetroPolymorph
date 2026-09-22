package dev.sosea1.retropolymorph.mixin.compat.enderio;

import dev.sosea1.retropolymorph.compat.enderio.EnderIoCrafterSelectionStore;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/** Seeds Ender IO's temporary crafting matrices with the tile's selected recipe. */
@Pseudo
@Mixin(targets = "crazypants.enderio.machines.machine.crafter.TileCrafter", remap = false)
public abstract class EnderIoTileCrafterMixin {

    @Unique
    @Nullable
    private InventoryCrafting retropolymorph$craftMatrix;

    @Unique
    @Nullable
    private InventoryCrafting retropolymorph$previewMatrix;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$craftSelection;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$previewSelection;

    @ModifyVariable(
            method = "craftRecipe",
            at = @At("STORE"),
            ordinal = 0,
            remap = false,
            require = 1)
    private InventoryCrafting retropolymorph$seedRealCraft(InventoryCrafting matrix) {
        if (!PolymorphConfig.isIntegrationEnderIoEnabled()) {
            return matrix;
        }
        TileEntity tile = (TileEntity) (Object) this;
        ResourceLocation selected = EnderIoCrafterSelectionStore.getSelectedRecipeId(tile);
        this.retropolymorph$craftMatrix = matrix;
        this.retropolymorph$craftSelection = selected;
        RecipeSelectionSeeder.seed(matrix, selected);
        return matrix;
    }

    @Inject(method = "craftRecipe", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$finishRealCraft(CallbackInfoReturnable<Boolean> cir) {
        retropolymorph$finishResolution(
                this.retropolymorph$craftMatrix,
                this.retropolymorph$craftSelection);
        this.retropolymorph$craftMatrix = null;
        this.retropolymorph$craftSelection = null;
    }

    @ModifyVariable(
            method = "updateCraftingOutput",
            at = @At("STORE"),
            ordinal = 0,
            remap = false,
            require = 1)
    private InventoryCrafting retropolymorph$seedPreview(InventoryCrafting matrix) {
        if (!PolymorphConfig.isIntegrationEnderIoEnabled()) {
            return matrix;
        }
        TileEntity tile = (TileEntity) (Object) this;
        ResourceLocation selected = EnderIoCrafterSelectionStore.getSelectedRecipeId(tile);
        this.retropolymorph$previewMatrix = matrix;
        this.retropolymorph$previewSelection = selected;
        RecipeSelectionSeeder.seed(matrix, selected);
        return matrix;
    }

    @Inject(method = "updateCraftingOutput", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$finishPreview(CallbackInfo ci) {
        retropolymorph$finishResolution(
                this.retropolymorph$previewMatrix,
                this.retropolymorph$previewSelection);
        this.retropolymorph$previewMatrix = null;
        this.retropolymorph$previewSelection = null;
    }

    @Unique
    private void retropolymorph$finishResolution(
            @Nullable InventoryCrafting matrix,
            @Nullable ResourceLocation selected) {
        if (selected == null || matrix == null) {
            return;
        }
        if (!RecipeSelectionSeeder.wasOutputResolutionObserved(matrix, selected)) {
            EnderIoCrafterSelectionStore.clearIfSelected((TileEntity) (Object) this, selected);
        }
    }
}
