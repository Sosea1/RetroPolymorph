package dev.sosea1.retropolymorph.mixin.compat.mekanism;

import dev.sosea1.retropolymorph.compat.mekanism.MekanismFormulaicSelectionStore;
import dev.sosea1.retropolymorph.compat.mekanism.MekanismFormulaicTileAccess;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import mekanism.common.content.assemblicator.RecipeFormula;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** Makes Mekanism's native cachedRecipe honor the shared RetroPolymorph selection. */
@Pseudo
@Mixin(value = TileEntityFormulaicAssemblicator.class, remap = false)
public abstract class MekanismFormulaicTileMixin implements MekanismFormulaicTileAccess {

    @Shadow(remap = false)
    public InventoryCrafting dummyInv;

    @Shadow(remap = false)
    public RecipeFormula formula;

    @Shadow(remap = false)
    private IRecipe cachedRecipe;

    @Unique
    private boolean retropolymorph$persistingStaleClear;

    @Inject(method = "recalculateRecipe", at = @At("HEAD"), remap = false)
    private void retropolymorph$seedPersistedManualSelection(CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationMekanismEnabled()
                || this.formula != null
                || this.dummyInv == null) {
            return;
        }
        ResourceLocation selected = MekanismFormulaicSelectionStore.getSelectedRecipeId(
                (TileEntity) (Object) this);
        RecipeSelectionSeeder.seed(this.dummyInv, selected);
    }


    @Inject(method = "recalculateRecipe", at = @At("RETURN"), remap = false)
    private void retropolymorph$dropStaleManualSelection(CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationMekanismEnabled() || this.formula != null) {
            return;
        }
        TileEntity tile = (TileEntity) (Object) this;
        ResourceLocation selected = MekanismFormulaicSelectionStore.getSelectedRecipeId(tile);
        if (selected == null) {
            return;
        }
        ResourceLocation resolved = this.cachedRecipe == null ? null : this.cachedRecipe.getRegistryName();
        if (!selected.equals(resolved)) {
            // The manual matrix changed and the previously selected recipe no
            // longer matches. Do not keep a stale machine-level preference.
            MekanismFormulaicSelectionStore.writeSelectedRecipeId(tile, null);
            // recalculateRecipe() also runs from onLoad(), where the chunk is
            // not necessarily dirty. Persist the cleared ForgeData through the
            // tile's native markDirty(); guard the one recursive recalc.
            if (!this.retropolymorph$persistingStaleClear) {
                this.retropolymorph$persistingStaleClear = true;
                try {
                    tile.markDirty();
                } finally {
                    this.retropolymorph$persistingStaleClear = false;
                }
            }
        }
    }

    @Override
    @Nullable
    public InventoryCrafting retropolymorph$getFormulaicDummyMatrix() {
        return this.dummyInv;
    }

    @Override
    public boolean retropolymorph$hasFormula() {
        return this.formula != null;
    }

    @Override
    public void retropolymorph$invalidateAndRecalculateRecipe() {
        this.cachedRecipe = null;
        // Mekanism's own markDirty() persists the tile and invokes exactly one
        // recalculateRecipe(). Its HEAD injection above seeds the selected ID
        // before Mekanism evaluates cachedRecipe / CraftingManager.
        ((TileEntity) (Object) this).markDirty();
    }
}
