package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
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
 * Adapter context for AE2 UEL crafting terminals and wireless wrappers.
 *
 * <p>The visible AE2 grid often is not itself an InventoryCrafting. A reusable
 * mirror is used only for recipe matching, while selection is stored against
 * the actual Container. The optional extension is still used when available to
 * keep AE2's currentRecipe field aligned, but it is no longer required for a
 * valid crafting-terminal topology.</p>
 */
final class Ae2CraftingTermContext implements RecipeSelectionContext {

    private static final Container MIRROR_OWNER = new MirrorContainer();

    private final Container container;
    @Nullable
    private final Ae2CraftingTermExtension extension;
    private final Slot[] inputSlots;
    private final Slot resultSlot;
    private final boolean wireless;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);

    Ae2CraftingTermContext(
            Container container,
            @Nullable Ae2CraftingTermExtension extension,
            Slot[] inputSlots,
            Slot resultSlot,
            boolean wireless) {
        this.container = container;
        this.extension = extension;
        this.inputSlots = inputSlots;
        this.resultSlot = resultSlot;
        this.wireless = wireless;
        refreshMatrix();

        if (extension != null) {
            ResourceLocation selected = extension.retropolymorph$getAe2SelectedRecipeId();
            if (selected != null) {
                Ae2SelectionStore.set(container, selected);
            }
        }
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
    public dev.sosea1.retropolymorph.api.SelectorPlacement getSelectorPlacement() {
        return dev.sosea1.retropolymorph.api.SelectorPlacement.resultSlot(
                this.resultSlot.xPos,
                this.resultSlot.yPos,
                this.wireless ? 18 : 0,
                this.wireless ? 8 : 0);
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
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }

        setSelection(recipeId, recipe);
        seedNativeMatrices(recipeId);
        putSelectedResult(recipe);
        return true;
    }

    @Override
    public void clearSelection() {
        ResourceLocation selected = getSelectedRecipeIdInternal();
        if (selected == null) {
            clearVisibleResultIfGridEmpty();
            return;
        }

        setSelection(null, null);
        clearNativeMatrices();
        clearVisibleResultIfGridEmpty();
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
            setSelection(null, null);
            clearNativeMatrices();
            clearVisibleResultIfGridEmpty();
            return;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null) {
            return;
        }

        refreshMatrix();
        setSelection(recipeId, recipe);
        seedNativeMatrices(recipeId);
        putSelectedResult(recipe);
    }

    private void setSelection(@Nullable ResourceLocation recipeId, @Nullable IRecipe recipe) {
        Ae2SelectionStore.set(this.container, recipeId);
        if (this.extension != null) {
            this.extension.retropolymorph$setAe2SelectedRecipeId(recipeId);
            this.extension.retropolymorph$setAe2CurrentRecipe(recipe);
        }
    }

    @Nullable
    private ResourceLocation getSelectedRecipeIdInternal() {
        ResourceLocation selected = Ae2SelectionStore.get(this.container);
        if (selected != null) {
            return selected;
        }
        if (this.extension != null) {
            selected = this.extension.retropolymorph$getAe2SelectedRecipeId();
            if (selected != null) {
                Ae2SelectionStore.set(this.container, selected);
            }
        }
        return selected;
    }

    private void seedNativeMatrices(ResourceLocation recipeId) {
        InventoryCrafting last = null;
        for (Slot slot : this.inputSlots) {
            if (!(slot.inventory instanceof InventoryCrafting)) {
                continue;
            }
            InventoryCrafting nativeMatrix = (InventoryCrafting) slot.inventory;
            if (nativeMatrix != last) {
                RecipeSelectionSeeder.seed(nativeMatrix, recipeId);
                last = nativeMatrix;
            }
        }
    }

    private void clearNativeMatrices() {
        InventoryCrafting last = null;
        for (Slot slot : this.inputSlots) {
            if (!(slot.inventory instanceof InventoryCrafting)) {
                continue;
            }
            InventoryCrafting nativeMatrix = (InventoryCrafting) slot.inventory;
            if (nativeMatrix != last) {
                RecipeSelectionSeeder.clear(nativeMatrix);
                last = nativeMatrix;
            }
        }
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

    private void putSelectedResult(IRecipe recipe) {
        refreshMatrix();
        if (isGridEmpty()) {
            this.resultSlot.putStack(ItemStack.EMPTY);
            return;
        }

        // Do not ask the terminal to recalculate before writing the selected
        // result. Native AE2 and WCT both default to the first matching recipe
        // in that recalculation, which was the source of the misleading preview.
        ItemStack outputStack = RecipeProbe.craftingResult(recipe, this.matrix);
        this.resultSlot.putStack(outputStack);
    }

    private void clearVisibleResultIfGridEmpty() {
        refreshMatrix();
        if (isGridEmpty()) {
            this.resultSlot.putStack(ItemStack.EMPTY);
        }
    }

    private boolean isGridEmpty() {
        for (Slot slot : this.inputSlots) {
            if (!slot.getStack().isEmpty()) {
                return false;
            }
        }
        return true;
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
