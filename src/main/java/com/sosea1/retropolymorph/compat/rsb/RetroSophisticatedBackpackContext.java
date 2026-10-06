package com.sosea1.retropolymorph.compat.rsb;

import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.api.RecipeSelectionContext;
import com.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import com.sosea1.retropolymorph.api.SelectorPlacement;
import com.sosea1.retropolymorph.core.CraftingMatrixExtension;
import com.sosea1.retropolymorph.core.CraftingRules;
import com.sosea1.retropolymorph.core.RecipeProbe;
import com.sosea1.retropolymorph.core.RecipeResolver;
import com.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One visible crafting-upgrade panel inside a Retro Sophisticated Backpack. */
final class RetroSophisticatedBackpackContext implements RecipeSelectionContext {

    private final Container container;
    private final InventoryCrafting unavailableMatrix = new InventoryCrafting(new UnownedContainer(), 0, 0);
    private RsbCraftingAccess.ActiveCrafting active;
    private ItemStack boundUpgradeStack = ItemStack.EMPTY;
    private NBTTagCompound boundUpgradeTag;

    RetroSophisticatedBackpackContext(
            Container container,
            RsbCraftingAccess.ActiveCrafting active) {
        this.container = container;
        this.active = active;
        captureUpgrade(active);
        refreshMatrix();
    }

    @Nullable
    private RsbCraftingAccess.ActiveCrafting currentActive() {
        if (this.active != null && isStillActive(this.active)) {
            return this.active;
        }
        RsbCraftingAccess.ActiveCrafting current =
                RsbCraftingAccess.resolveActiveCrafting(this.container);
        this.active = current;
        captureUpgrade(current);
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
        Map<?, ?> matrices = RsbBindings.getMatrices(this.container);
        InventoryCrafting mapped = matrices == null ? null
                : RsbCraftingAccess.matrixFromMap(matrices, active.upgradeIndex);
        if ((mapped != null || (matrices != null && !active.slotFallback)) && mapped != active.matrix) {
            return false;
        }
        Map<?, ?> results = RsbBindings.getResults(this.container);
        Slot mappedResult = results == null ? null
                : RsbCraftingAccess.resultSlotFromMap(results, active.upgradeIndex);
        if (mappedResult != null && mappedResult != active.resultSlot) {
            return false;
        }
        if (active.isClientMirror()) {
            ItemStack stack = upgradeStack(active.upgradeIndex);
            NBTTagCompound tag = stack.getTagCompound();
            if (stack != this.boundUpgradeStack
                    || (this.boundUpgradeTag == null ? tag != null : !this.boundUpgradeTag.equals(tag))) {
                return false;
            }
            IItemHandler handler = RsbBindings.extractCraftMatrixFromStack(stack);
            if (handler != null && handler != active.clientHandler) {
                return false;
            }
        }
        if (installed.size() > 1) {
            return RsbContainerAccess.isCandidateTabOpened(backpackWrapper, active.upgradeIndex);
        }
        return true;
    }

    private void captureUpgrade(@Nullable RsbCraftingAccess.ActiveCrafting active) {
        this.boundUpgradeStack = active == null ? ItemStack.EMPTY : upgradeStack(active.upgradeIndex);
        NBTTagCompound tag = this.boundUpgradeStack.getTagCompound();
        this.boundUpgradeTag = tag == null ? null : tag.copy();
    }

    private ItemStack upgradeStack(int index) {
        Object wrapper = RsbContainerAccess.readBackpackWrapper(this.container);
        Object value = wrapper == null ? null : RsbBindings.invokeNoArg(wrapper, "getUpgradeItemStackHandler");
        if (value instanceof IItemHandler) {
            IItemHandler handler = (IItemHandler) value;
            if (index >= 0 && index < handler.getSlots()) {
                ItemStack stack = handler.getStackInSlot(index);
                return stack == null ? ItemStack.EMPTY : stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        RsbCraftingAccess.ActiveCrafting active = refreshMatrix();
        return active == null ? this.unavailableMatrix : active.matrix;
    }

    @Override
    @Nullable
    public Slot getResultSlot() {
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        return active == null ? null : active.resultSlot;
    }

    @Override
    public int getInputCount() {
        // RSB's IndexedInventoryCraftingWrapper owns a synthetic slot 9 for
        // the synced output. Only the 3x3 input region participates in the
        // conflict fingerprint/query. Keeping slot 9 out also prevents output
        // changes from retriggering the selector as if they were new inputs.
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        return active == null ? 0 : Math.min(9, active.matrix.getSizeInventory());
    }

    @Override
    public ItemStack getInputStack(int index) {
        RsbCraftingAccess.ActiveCrafting active = refreshMatrix();
        return active != null && index >= 0 && index < Math.min(9, active.matrix.getSizeInventory())
                ? active.matrix.getStackInSlot(index)
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
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        return active == null ? 0 : 0x52534200 ^ active.upgradeIndex;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        RsbCraftingAccess.ActiveCrafting active = refreshMatrix();
        if (active == null) {
            return Collections.emptyList();
        }
        InventoryCrafting matrix = active.matrix;
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
        RsbCraftingAccess.ActiveCrafting active = refreshMatrix();
        if (active == null) {
            return false;
        }
        InventoryCrafting matrix = active.matrix;
        if (CraftingRules.isRepairCombination(matrix)) {
            return false;
        }
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        IRecipe recipe = id == null ? null : ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null || !RecipeProbe.matches(recipe, matrix, world)) {
            return false;
        }

        state(active).select(id);
        IItemHandler handler = RsbSlotAccess.extractMatrixHandler(active);
        RsbSlotAccess.storeSelection(handler, id);
        refreshNativeOutput(recipe);
        return true;
    }

    @Override
    public void clearSelection() {
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        if (active == null) {
            return;
        }
        state(active).clear();
        IItemHandler handler = RsbSlotAccess.extractMatrixHandler(active);
        RsbSlotAccess.storeSelection(handler, null);
        refreshNativeOutput(null);
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        if (active == null) {
            return null;
        }
        RecipeSelectionState state = state(active);
        ResourceLocation id = state == null ? null : state.getSelectedRecipeId();
        if (id == null) {
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
        if (active == null) {
            return;
        }
        IItemHandler handler = RsbSlotAccess.extractMatrixHandler(active);
        if (recipeKey == null) {
            state(active).clear();
            RsbSlotAccess.storeSelection(handler, null);
            refreshNativeOutput(null);
            return;
        }
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        state(active).select(id);
        RsbSlotAccess.storeSelection(handler, id);
        IRecipe recipe = id == null ? null : ForgeRegistries.RECIPES.getValue(id);
        refreshNativeOutput(recipe);
    }


    private void refreshNativeOutput(@Nullable IRecipe selectedRecipe) {
        RsbCraftingAccess.ActiveCrafting active = refreshMatrix();
        if (active == null) {
            return;
        }
        if (active.isClientMirror()) {
            if (isGridEmpty(active.matrix)) {
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

    @Nullable
    private RsbCraftingAccess.ActiveCrafting refreshMatrix() {
        RsbCraftingAccess.ActiveCrafting active = currentActive();
        RsbSlotAccess.refreshClientMatrix(active);
        return active;
    }

    private static boolean isGridEmpty(InventoryCrafting matrix) {
        for (int index = 0; index < Math.min(9, matrix.getSizeInventory()); index++) {
            if (!matrix.getStackInSlot(index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static RecipeSelectionState state(RsbCraftingAccess.ActiveCrafting active) {
        return ((CraftingMatrixExtension) active.matrix).retropolymorph$getOrCreateRecipeSelectionState();
    }

    private static final class UnownedContainer extends Container {
        @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return false; }
        @Override public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) { }
    }
}
