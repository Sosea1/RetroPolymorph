package dev.sosea1.retropolymorph.compat.rsb;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingRules;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** One visible crafting-upgrade panel inside a Retro Sophisticated Backpack. */
final class RetroSophisticatedBackpackContext implements RecipeSelectionContext {

    private final Container container;
    private RsbCraftingAccess.ActiveCrafting active;

    RetroSophisticatedBackpackContext(
            Container container,
            RsbCraftingAccess.ActiveCrafting active) {
        this.container = container;
        this.active = active;
        refreshMatrix();
    }

    private RsbCraftingAccess.ActiveCrafting currentActive() {
        if (this.active != null && isStillActive(this.active)) {
            return this.active;
        }
        RsbCraftingAccess.ActiveCrafting current =
                RsbCraftingAccess.resolveActiveCrafting(this.container);
        if (current != null) {
            this.active = current;
        }
        return this.active;
    }

    private boolean isStillActive(RsbCraftingAccess.ActiveCrafting active) {
        if (active == null) {
            return false;
        }
        Object backpackWrapper = RsbContainerAccess.readBackpackWrapper(this.container);
        if (backpackWrapper == null) {
            return false;
        }
        Set<Integer> installed = RsbContainerAccess.findInstalledCraftingUpgradeIndices(backpackWrapper);
        if (!installed.contains(Integer.valueOf(active.upgradeIndex))) {
            return false;
        }
        if (installed.size() > 1) {
            return RsbContainerAccess.isCandidateTabOpened(backpackWrapper, active.upgradeIndex);
        }
        return true;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        refreshMatrix();
        return currentActive().matrix;
    }

    @Override
    public Slot getResultSlot() {
        return currentActive().resultSlot;
    }

    @Override
    public int getInputCount() {
        // RSB's IndexedInventoryCraftingWrapper owns a synthetic slot 9 for
        // the synced output. Only the 3x3 input region participates in the
        // conflict fingerprint/query. Keeping slot 9 out also prevents output
        // changes from retriggering the selector as if they were new inputs.
        return Math.min(9, currentActive().matrix.getSizeInventory());
    }

    @Override
    public ItemStack getInputStack(int index) {
        refreshMatrix();
        return index >= 0 && index < getInputCount()
                ? currentActive().matrix.getStackInSlot(index)
                : ItemStack.EMPTY;
    }

    @Override
    public SelectorPlacement getSelectorPlacement() {
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        if (active == null) {
            return SelectorPlacement.hidden();
        }
        if (active.isClientMirror()) {
            Boolean expanded = RsbGuiAccess.isCraftingTabExpanded(this.container);
            if (expanded != null && !expanded.booleanValue()) {
                return SelectorPlacement.hidden();
            }
        }
        Point pt = RsbGuiAccess.findCraftingButtonPosition(this.container);
        if (pt != null) {
            return SelectorPlacement.absolute(pt.x, pt.y);
        }
        if (active.resultSlot != null) {
            return SelectorPlacement.resultSlot(active.resultSlot.xPos + 18, active.resultSlot.yPos - 19);
        }
        return SelectorPlacement.guiTopRight(6, 6);
    }

    @Override
    public SelectionPersistencePolicy getPersistencePolicy() {
        return SelectionPersistencePolicy.PLAYER_PERSISTENT;
    }

    @Override
    public int getClientStateToken() {
        // Switching between multiple crafting upgrades changes the backing
        // matrix even when the visible stacks happen to be identical.
        return 0x52534200 ^ currentActive().upgradeIndex;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        refreshMatrix();
        InventoryCrafting matrix = currentActive().matrix;
        if (CraftingRules.isRepairCombination(matrix)) {
            return Collections.emptyList();
        }
        return RecipeResolver.findAllMatches(matrix, world);
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation id = recipe.getRegistryName();
        return id == null ? null : id.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        refreshMatrix();
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        InventoryCrafting matrix = active.matrix;
        if (CraftingRules.isRepairCombination(matrix)) {
            return false;
        }
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        IRecipe recipe = id == null ? null : ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null || !RecipeProbe.matches(recipe, matrix, world)) {
            return false;
        }

        state().select(id);
        IItemHandler handler = RsbSlotAccess.extractMatrixHandler(active);
        RsbSlotAccess.storeSelection(handler, id);
        refreshNativeOutput(recipe);
        return true;
    }

    @Override
    public void clearSelection() {
        state().clear();
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        IItemHandler handler = RsbSlotAccess.extractMatrixHandler(active);
        RsbSlotAccess.storeSelection(handler, null);
        refreshNativeOutput(null);
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        RecipeSelectionState state = state();
        ResourceLocation id = state == null ? null : state.getSelectedRecipeId();
        if (id == null) {
            RsbCraftingAccess.ActiveCrafting active = currentActive();
            IItemHandler handler = RsbSlotAccess.extractMatrixHandler(active);
            id = RsbSlotAccess.getStoredSelection(handler);
            if (id != null && state != null) {
                state.select(id);
            }
        }
        return id == null ? null : id.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        IItemHandler handler = RsbSlotAccess.extractMatrixHandler(active);
        if (recipeKey == null) {
            state().clear();
            RsbSlotAccess.storeSelection(handler, null);
            refreshNativeOutput(null);
            return;
        }
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        state().select(id);
        RsbSlotAccess.storeSelection(handler, id);
        IRecipe recipe = id == null ? null : ForgeRegistries.RECIPES.getValue(id);
        refreshNativeOutput(recipe);
    }


    private void refreshNativeOutput(@Nullable IRecipe selectedRecipe) {
        refreshMatrix();
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        if (active.isClientMirror()) {
            if (isGridEmpty()) {
                active.resultSlot.putStack(ItemStack.EMPTY);
                return;
            }
            if (selectedRecipe != null) {
                active.resultSlot.putStack(RecipeProbe.craftingResult(selectedRecipe, active.matrix));
            }
            return;
        }
        this.container.onCraftMatrixChanged(active.matrix);
    }

    private void refreshMatrix() {
        RsbSlotAccess.refreshClientMatrix(currentActive());
    }

    private boolean isGridEmpty() {
        InventoryCrafting matrix = currentActive().matrix;
        for (int index = 0; index < getInputCount(); index++) {
            if (!matrix.getStackInSlot(index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private CraftingMatrixExtension extension() {
        return (CraftingMatrixExtension) currentActive().matrix;
    }

    private RecipeSelectionState state() {
        return extension().retropolymorph$getOrCreateRecipeSelectionState();
    }
}
