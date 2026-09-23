package dev.sosea1.retropolymorph.compat.cyclic;

import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.core.CraftingRecipeOptionCollector;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Machine surface implementation for Cyclic's Auto-Crafter ("Авто-верстак"). */
final class CyclicCrafterSurface implements MachineRecipeSurface {

    private static final Container MIRROR_OWNER = new Container() {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    };

    private final Container container;
    private final TileEntity tile;
    private final CyclicSelectionAccess selection;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);

    CyclicCrafterSurface(Container container, TileEntity tile) {
        this.container = container;
        this.tile = tile;
        this.selection = tile instanceof CyclicSelectionAccess ? (CyclicSelectionAccess) tile : null;
        refreshMatrix();
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public Object getRecipeOwner() {
        return this.tile;
    }

    @Override
    public int getInputCount() {
        return 9;
    }

    @Override
    public ItemStack getInputStack(int index) {
        if (index < 0 || index >= 9) {
            return ItemStack.EMPTY;
        }
        int slotIndex = 10 + index;
        if (slotIndex >= this.container.inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        return this.container.inventorySlots.get(slotIndex).getStack();
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        if (world == null || isGridEmpty()) {
            return Collections.emptyList();
        }
        refreshMatrix();
        List<IRecipe> matches = RecipeResolver.findAllMatches(this.matrix, world);
        return CraftingRecipeOptionCollector.collectForge(this.matrix, matches);
    }

    @Override
    public boolean selectRecipe(String recipeKey, World world) {
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id == null || world == null || this.selection == null) {
            return false;
        }
        refreshMatrix();
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }
        this.selection.retropolymorph$setSelectedRecipeId(id);
        this.tile.markDirty();
        return true;
    }

    @Override
    public void clearRecipeSelection() {
        if (this.selection != null) {
            this.selection.retropolymorph$setSelectedRecipeId(null);
            this.tile.markDirty();
        }
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        if (this.selection == null) {
            return null;
        }
        ResourceLocation id = this.selection.retropolymorph$getSelectedRecipeId();
        return id == null ? null : id.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        if (this.selection == null) {
            return;
        }
        this.selection.retropolymorph$setSelectedRecipeId(RecipeKey.parseForgeId(recipeKey));
    }

    @Override
    public MachineRecipePersistence getPersistencePolicy() {
        return MachineRecipePersistence.OWNER;
    }

    @Override
    public boolean controlsActualOperation() {
        return true;
    }

    @Override
    public boolean retainSelectionWhenOptionsEmpty() {
        // Keep the saved recipe selection while the template inventory is temporarily
        // empty (e.g. during a template swap). RecipeSelectionState.resolveSelected()
        // will still validate via RecipeProbe.matches() before applying the recipe.
        return true;
    }

    @Override
    @Nullable
    public Slot getResultSlot() {
        return null;
    }

    @Override
    public SelectorPlacement getSelectorPlacement() {
        return SelectorPlacement.resultSlot(86, 63, 0, 0);
    }

    private void refreshMatrix() {
        for (int i = 0; i < 9; i++) {
            this.matrix.setInventorySlotContents(i, getInputStack(i));
        }
    }

    private boolean isGridEmpty() {
        for (int i = 0; i < 9; i++) {
            if (!getInputStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
