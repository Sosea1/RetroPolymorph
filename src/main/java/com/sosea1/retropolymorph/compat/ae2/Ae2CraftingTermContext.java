package com.sosea1.retropolymorph.compat.ae2;

import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.api.RecipeSelectionContext;
import com.sosea1.retropolymorph.core.RecipeProbe;
import com.sosea1.retropolymorph.core.RecipeResolver;
import com.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

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
    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    private final Container container;
    @Nullable
    private final Field currentRecipeField;
    @Nullable
    private final Ae2CraftingTermExtension extension;
    private final Slot[] inputSlots;
    private final Slot resultSlot;
    private final boolean wireless;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);
    @Nullable
    private ResourceLocation remoteRecipeId;
    @Nullable
    private ResourceLocation reconciledRecipeId;
    @Nullable
    private World reconciledWorld;
    @Nullable
    private ItemStack[] reconciledInputs;
    @Nullable
    private IRecipe reconciledRecipe;
    private ItemStack reconciledResult = ItemStack.EMPTY;
    private boolean remoteSelectionReady;

    Ae2CraftingTermContext(
            Container container,
            @Nullable Ae2CraftingTermExtension extension,
            Slot[] inputSlots,
            Slot resultSlot,
            boolean wireless) {
        this.container = container;
        this.currentRecipeField = findCurrentRecipeField(container.getClass());
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
    public com.sosea1.retropolymorph.api.SelectorPlacement getSelectorPlacement() {
        return com.sosea1.retropolymorph.api.SelectorPlacement.resultSlot(
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

        resetRemoteSelection();
        setSelection(recipeId, recipe);
        seedNativeMatrices(recipeId);
        putSelectedResult(recipe);
        refreshContainerOutput("select");
        return true;
    }

    @Override
    public void clearSelection() {
        resetRemoteSelection();
        setSelection(null, null);
        clearNativeMatrices();
        refreshMatrix();
        World world = this.container != null ? Ae2TerminalRecipePin.resolveWorld(this.container) : null;
        if (isGridEmpty()) {
            this.resultSlot.putStack(ItemStack.EMPTY);
        } else if (world != null) {
            IRecipe nativeRecipe = CraftingManager.findMatchingRecipe(this.matrix, world);
            if (nativeRecipe != null) {
                if (this.extension != null) {
                    this.extension.retropolymorph$setAe2CurrentRecipe(nativeRecipe);
                }
                setContainerCurrentRecipe(nativeRecipe);
                this.resultSlot.putStack(RecipeProbe.craftingResult(nativeRecipe, this.matrix));
            } else {
                if (this.extension != null) {
                    this.extension.retropolymorph$setAe2CurrentRecipe(null);
                }
                setContainerCurrentRecipe(null);
                this.resultSlot.putStack(ItemStack.EMPTY);
            }
        }
        refreshContainerOutput("clearSelection");
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation selected = this.remoteRecipeId != null
                ? this.remoteRecipeId : getSelectedRecipeIdInternal();
        return selected == null ? null : selected.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            clearSelection();
            return;
        }

        // Slot packets may arrive after this reply. Keep the authoritative ID
        // separately from AE2's native state, which partial matrix updates clear.
        this.remoteRecipeId = recipeId;
        this.reconciledInputs = null;
        World world = Ae2TerminalRecipePin.resolveWorld(this.container);
        if (world != null) {
            reconcileRemoteSelection(recipeKey, world);
        }
    }

    @Override
    public boolean reconcileRemoteSelection(@Nullable String recipeKey, World world) {
        if (recipeKey == null) {
            return true;
        }
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }
        this.remoteRecipeId = recipeId;
        refreshMatrix();
        boolean changed = !recipeId.equals(this.reconciledRecipeId)
                || world != this.reconciledWorld || !sameReconciledInputs();
        if (changed) {
            this.reconciledRecipeId = recipeId;
            this.reconciledWorld = world;
            this.reconciledInputs = new ItemStack[this.inputSlots.length];
            for (int i = 0; i < this.reconciledInputs.length; i++) {
                this.reconciledInputs[i] = this.inputSlots[i].getStack().copy();
            }
            this.reconciledRecipe = ForgeRegistries.RECIPES.getValue(recipeId);
            this.remoteSelectionReady = !isGridEmpty() && this.reconciledRecipe != null
                    && RecipeProbe.matches(this.reconciledRecipe, this.matrix, world);
            this.reconciledResult = this.remoteSelectionReady
                    ? RecipeProbe.craftingResult(this.reconciledRecipe, this.matrix) : ItemStack.EMPTY;
            if (!this.remoteSelectionReady) {
                LOGGER.debug("AE2 selection deferred: container={}, window={}, selected={}, mismatchingGrid=true",
                        this.container.getClass().getName(), this.container.windowId, recipeKey);
            }
        }
        if (!this.remoteSelectionReady) {
            return false;
        }

        boolean stateChanged = !Objects.equals(recipeId, Ae2SelectionStore.get(this.container))
                || !hasContainerCurrentRecipe(this.reconciledRecipe)
                || (this.extension != null
                && (!recipeId.equals(this.extension.retropolymorph$getAe2SelectedRecipeId())
                || this.extension.retropolymorph$getAe2CurrentRecipe() != this.reconciledRecipe));
        boolean resultChanged = !ItemStack.areItemStacksEqual(
                this.resultSlot.getStack(), this.reconciledResult);
        if (changed || stateChanged || resultChanged) {
            setSelection(recipeId, this.reconciledRecipe);
            seedNativeMatrices(recipeId);
            if (resultChanged) {
                this.resultSlot.putStack(this.reconciledResult.copy());
            }
        }
        return true;
    }

    private boolean sameReconciledInputs() {
        if (this.reconciledInputs == null) {
            return false;
        }
        for (int i = 0; i < this.reconciledInputs.length; i++) {
            if (!ItemStack.areItemStacksEqual(this.reconciledInputs[i], this.inputSlots[i].getStack())) {
                return false;
            }
        }
        return true;
    }

    private void resetRemoteSelection() {
        this.remoteRecipeId = null;
        this.reconciledRecipeId = null;
        this.reconciledWorld = null;
        this.reconciledInputs = null;
        this.reconciledRecipe = null;
        this.reconciledResult = ItemStack.EMPTY;
        this.remoteSelectionReady = false;
    }

    private void refreshContainerOutput(String phase) {
        if (this.container != null) {
            Ae2MatrixChangeScope.enter(this.container);
            try {
                this.container.onCraftMatrixChanged(null);
            } catch (Throwable ignored) {
            } finally {
                Ae2MatrixChangeScope.exit();
            }
            Ae2TerminalRecipePin.handleMatrixChangedReturn(this.container, phase);
        }
    }

    private void setSelection(@Nullable ResourceLocation recipeId, @Nullable IRecipe recipe) {
        Ae2SelectionStore.set(this.container, recipeId);
        if (this.extension != null) {
            this.extension.retropolymorph$setAe2SelectedRecipeId(recipeId);
            this.extension.retropolymorph$setAe2CurrentRecipe(recipe);
        }
        setContainerCurrentRecipe(recipe);
    }

    private void setContainerCurrentRecipe(@Nullable IRecipe recipe) {
        if (this.currentRecipeField == null) {
            return;
        }
        try {
            this.currentRecipeField.set(this.container, recipe);
        } catch (IllegalAccessException exception) {
            LOGGER.debug("AE2 currentRecipe write failed", exception);
        }
    }

    private boolean hasContainerCurrentRecipe(@Nullable IRecipe recipe) {
        if (this.currentRecipeField == null) {
            return true;
        }
        try {
            return this.currentRecipeField.get(this.container) == recipe;
        } catch (IllegalAccessException exception) {
            return true; // An inaccessible optional bridge must not trigger a repair loop.
        }
    }

    @Nullable
    private static Field findCurrentRecipeField(Class<?> clazz) {
        while (clazz != null && clazz != Object.class && clazz != Container.class) {
            try {
                Field field = clazz.getDeclaredField("currentRecipe");
                if (IRecipe.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException | RuntimeException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
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

        World world = this.container != null ? Ae2TerminalRecipePin.resolveWorld(this.container) : null;
        if (world != null && !RecipeProbe.matches(recipe, this.matrix, world)) {
            this.resultSlot.putStack(ItemStack.EMPTY);
            return;
        }

        ItemStack outputStack = RecipeProbe.craftingResult(recipe, this.matrix);
        this.resultSlot.putStack(outputStack);
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
