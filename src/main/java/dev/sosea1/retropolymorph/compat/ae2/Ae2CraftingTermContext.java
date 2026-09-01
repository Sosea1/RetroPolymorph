package dev.sosea1.retropolymorph.compat.ae2;

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
import java.util.List;

/**
 * Adapter context for AE2 UEL's crafting terminal.
 *
 * AE2 stores its grid in IItemHandler-backed slots and rebuilds a temporary
 * InventoryCrafting whenever it resolves recipes. This context keeps one small
 * reusable mirror for matching/UI while the actual selected recipe remains in
 * AE2's own currentRecipe field through the soft bridge.
 */
final class Ae2CraftingTermContext implements RecipeSelectionContext {

    private static final Container MIRROR_OWNER = new MirrorContainer();

    private final Container container;
    private final Ae2CraftingTermExtension extension;
    private final Slot[] inputSlots;
    private final Slot resultSlot;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);

    Ae2CraftingTermContext(
            Container container,
            Ae2CraftingTermExtension extension,
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
    public List<IRecipe> findAllMatches(World world) {
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
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }

        refreshMatrix();
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null || !recipe.matches(this.matrix, world)) {
            return false;
        }

        this.extension.retropolymorph$setAe2SelectedRecipeId(recipeId);
        this.extension.retropolymorph$setAe2CurrentRecipe(recipe);
        refreshOutput();
        return true;
    }

    @Override
    public void clearSelection() {
        if (getSelectedRecipeIdInternal() == null) {
            return;
        }

        this.extension.retropolymorph$setAe2SelectedRecipeId(null);
        this.extension.retropolymorph$setAe2CurrentRecipe(null);
        refreshOutput();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation selected = getSelectedRecipeIdInternal();
        return selected == null ? null : selected.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            this.extension.retropolymorph$setAe2SelectedRecipeId(null);
            this.extension.retropolymorph$setAe2CurrentRecipe(null);
            refreshOutput();
            return;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null) {
            return;
        }

        this.extension.retropolymorph$setAe2SelectedRecipeId(recipeId);
        this.extension.retropolymorph$setAe2CurrentRecipe(recipe);
        refreshOutput();
    }

    @Nullable
    private ResourceLocation getSelectedRecipeIdInternal() {
        return this.extension.retropolymorph$getAe2SelectedRecipeId();
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

    private void refreshOutput() {
        this.container.onCraftMatrixChanged(this.matrix);
        IRecipe recipe = this.extension.retropolymorph$getAe2CurrentRecipe();
        if (recipe != null) {
            refreshMatrix();
            ItemStack outputStack = recipe.getCraftingResult(this.matrix);
            this.resultSlot.putStack(outputStack);
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
