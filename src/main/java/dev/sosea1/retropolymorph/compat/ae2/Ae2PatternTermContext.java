package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Recipe-selection backend for AE2 UEL's Pattern Terminal while it is in
 * crafting-pattern mode.
 *
 * The terminal stores fake inputs rather than a persistent InventoryCrafting,
 * so one small mirror is reused for matching and GUI snapshots. Optional AE2
 * mixins seed the same selected recipe into AE2's own temporary matrices when
 * it computes the preview or directly crafts a pattern request.
 */
final class Ae2PatternTermContext implements RecipeSelectionContext {

    private static final Container MIRROR_OWNER = new MirrorContainer();

    private final Container container;
    private final Ae2PatternTermExtension extension;
    private final Slot[] inputSlots;
    private final Slot resultSlot;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);

    Ae2PatternTermContext(
            Container container,
            Ae2PatternTermExtension extension,
            Slot[] inputSlots,
            Slot resultSlot) {
        this.container = container;
        this.extension = extension;
        this.inputSlots = inputSlots;
        this.resultSlot = resultSlot;
        refreshMatrix();
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        refreshMatrix();
        return this.matrix;
    }

    @Override
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public int getClientStateToken() {
        return this.extension.retropolymorph$isPatternCraftingMode() ? 1 : 0;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        if (!this.extension.retropolymorph$isPatternCraftingMode()) {
            return Collections.emptyList();
        }

        refreshMatrix();
        return RecipeResolver.findAllMatches(this.matrix, world);
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation recipeId = recipe.getRegistryName();
        return recipeId == null ? null : recipeId.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        if (!this.extension.retropolymorph$isPatternCraftingMode()) {
            clearSelection();
            return false;
        }

        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }

        refreshMatrix();
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }

        ResourceLocation previous = this.extension.retropolymorph$getPatternSelectedRecipeId();
        this.extension.retropolymorph$setPatternSelectedRecipeId(recipeId);
        this.extension.retropolymorph$resetPatternPreviewObservation();
        this.extension.retropolymorph$refreshPatternOutput();

        if (this.extension.retropolymorph$wasPatternPreviewObserved()
                && recipeId.equals(this.extension.retropolymorph$getPatternSelectedRecipeId())) {
            return true;
        }

        this.extension.retropolymorph$setPatternSelectedRecipeId(previous);
        this.extension.retropolymorph$resetPatternPreviewObservation();
        this.extension.retropolymorph$refreshPatternOutput();
        return false;
    }

    @Override
    public void clearSelection() {
        if (this.extension.retropolymorph$getPatternSelectedRecipeId() == null) {
            return;
        }

        this.extension.retropolymorph$setPatternSelectedRecipeId(null);
        this.extension.retropolymorph$resetPatternPreviewObservation();
        this.extension.retropolymorph$refreshPatternOutput();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        if (!this.extension.retropolymorph$isPatternCraftingMode()) {
            this.extension.retropolymorph$setPatternSelectedRecipeId(null);
            return null;
        }

        ResourceLocation selected = this.extension.retropolymorph$getPatternSelectedRecipeId();
        return selected == null ? null : selected.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        this.extension.retropolymorph$setPatternSelectedRecipeId(recipeId);
        this.extension.retropolymorph$resetPatternPreviewObservation();
        this.extension.retropolymorph$refreshPatternOutput();
    }

    private void refreshMatrix() {
        for (int slot = 0; slot < this.inputSlots.length; slot++) {
            ItemStack source = this.inputSlots[slot].getStack();
            ItemStack mirrored = this.matrix.getStackInSlot(slot);
            if (mirrored != source && !ItemStack.areItemStacksEqual(mirrored, source)) {
                this.matrix.setInventorySlotContents(slot, source);
            }
        }
    }

    private static final class MirrorContainer extends Container {

        @Override
        public void onCraftMatrixChanged(IInventory inventory) {
            // Mirror updates are local bookkeeping, never container events.
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }
}
