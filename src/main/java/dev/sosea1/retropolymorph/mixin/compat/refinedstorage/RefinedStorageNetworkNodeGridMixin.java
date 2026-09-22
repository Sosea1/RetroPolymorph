package dev.sosea1.retropolymorph.mixin.compat.refinedstorage;

import dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStoragePatternData;
import dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStoragePatternGridStateAccess;
import dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStorageSelectionRefresh;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Invalidates Refined Storage's currentRecipe cache when the user changes the
 * Polymorph selection without changing the crafting matrix.
 */
@Pseudo
@Mixin(targets = "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid", remap = false)
public abstract class RefinedStorageNetworkNodeGridMixin implements RefinedStoragePatternGridStateAccess {

    @Shadow(remap = false)
    private IRecipe currentRecipe;

    @Shadow(remap = false)
    private InventoryCrafting matrix;

    @Shadow(remap = false)
    public abstract boolean isProcessingPattern();

    @Shadow(remap = false)
    public abstract IItemHandler getPatterns();

    @Unique
    private ItemStack retropolymorph$lastPatternSeed = ItemStack.EMPTY;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$pendingPatternRecipe;

    @Unique
    private boolean retropolymorph$patternSeedPendingRestore;

    @Override
    public boolean retropolymorph$isRefinedStorageProcessingPattern() {
        return isProcessingPattern();
    }


    @Unique
    private void retropolymorph$observePatternSeedChange() {
        IItemHandler patterns = getPatterns();
        if (patterns == null || patterns.getSlots() <= 1) {
            return;
        }

        ItemStack encoded = patterns.getStackInSlot(1);
        if (ItemStack.areItemStacksEqual(this.retropolymorph$lastPatternSeed, encoded)) {
            return;
        }
        this.retropolymorph$lastPatternSeed = encoded.isEmpty() ? ItemStack.EMPTY : encoded.copy();
        this.retropolymorph$patternSeedPendingRestore = !encoded.isEmpty();

        // RS loads a written pattern into the 3x3 matrix one slot at a time.
        // Applying the encoded recipe here would be too early: the first partial
        // matrix can fail recipe.matches() and clear RecipeSelectionState before
        // the remaining slots have been restored. Drop any previous transient
        // choice now and restore the encoded id only once the live matrix equals
        // all nine saved pattern inputs. During onCreatePattern
        // the pending field preserves the current selection until the new stack
        // can be tagged at TAIL.
        if (this.retropolymorph$pendingPatternRecipe == null) {
            RecipeSelectionState state =
                    ((CraftingMatrixExtension) this.matrix).retropolymorph$peekRecipeSelectionState();
            if (state != null) {
                state.clear();
            }
        }
    }

    @Unique
    private void retropolymorph$restorePatternSelectionIfMatrixReady() {
        if (!this.retropolymorph$patternSeedPendingRestore || isProcessingPattern()) {
            return;
        }

        IItemHandler patterns = getPatterns();
        if (patterns == null || patterns.getSlots() <= 1) {
            this.retropolymorph$patternSeedPendingRestore = false;
            return;
        }

        ItemStack encoded = patterns.getStackInSlot(1);
        ResourceLocation encodedRecipe = RefinedStoragePatternData.readSelectedRecipe(encoded);
        if (encodedRecipe == null) {
            this.retropolymorph$patternSeedPendingRestore = false;
            return;
        }

        // Official RS 1.6.16 copies a written pattern into InventoryCrafting one
        // slot at a time and fires onCraftingMatrixChanged after each copy. Only
        // restore once the live matrix exactly matches all nine encoded inputs.
        if (!RefinedStoragePatternData.matchesEncodedInputs(encoded, this.matrix)) {
            return;
        }

        RecipeSelectionState state =
                ((CraftingMatrixExtension) this.matrix).retropolymorph$getOrCreateRecipeSelectionState();
        state.select(encodedRecipe);
        this.currentRecipe = null;
        this.retropolymorph$patternSeedPendingRestore = false;
    }

    @Inject(method = "onCreatePattern", at = @At("HEAD"), remap = false, require = 1)
    private void retropolymorph$captureSelectedRecipeForPattern(CallbackInfo ci) {
        this.retropolymorph$pendingPatternRecipe = null;
        if (!PolymorphConfig.isIntegrationRefinedStorageEnabled() || isProcessingPattern()) {
            return;
        }

        RecipeSelectionState state =
                ((CraftingMatrixExtension) this.matrix).retropolymorph$peekRecipeSelectionState();
        this.retropolymorph$pendingPatternRecipe =
                state == null ? null : state.getSelectedRecipeId();
    }

    @Inject(method = "onCreatePattern", at = @At("TAIL"), remap = false, require = 1)
    private void retropolymorph$writeSelectedRecipeToPattern(CallbackInfo ci) {
        ResourceLocation selected = this.retropolymorph$pendingPatternRecipe;
        this.retropolymorph$pendingPatternRecipe = null;
        if (!PolymorphConfig.isIntegrationRefinedStorageEnabled()
                || isProcessingPattern()
                || selected == null) {
            return;
        }

        IItemHandler patterns = getPatterns();
        if (!(patterns instanceof IItemHandlerModifiable) || patterns.getSlots() <= 1) {
            return;
        }

        ItemStack encoded = patterns.getStackInSlot(1);
        if (encoded.isEmpty()) {
            return;
        }

        ItemStack tagged = encoded.copy();
        RefinedStoragePatternData.writeSelectedRecipe(tagged, selected);
        ((IItemHandlerModifiable) patterns).setStackInSlot(1, tagged);
        this.retropolymorph$lastPatternSeed = tagged.copy();
    }

    @Inject(method = "onCraftingMatrixChanged", at = @At("HEAD"), remap = false, require = 1)
    private void retropolymorph$invalidateSelectedRecipeCache(CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationRefinedStorageEnabled()) {
            return;
        }

        retropolymorph$observePatternSeedChange();
        retropolymorph$restorePatternSelectionIfMatrixReady();

        if (RefinedStorageSelectionRefresh.consume(this.matrix)) {
            this.currentRecipe = null;
            return;
        }

        RecipeSelectionState state =
                ((CraftingMatrixExtension) this.matrix).retropolymorph$peekRecipeSelectionState();
        ResourceLocation selected = state == null ? null : state.getSelectedRecipeId();
        if (selected == null || this.currentRecipe == null) {
            return;
        }

        ResourceLocation current = this.currentRecipe.getRegistryName();
        if (!selected.equals(current)) {
            this.currentRecipe = null;
        }
    }
}
