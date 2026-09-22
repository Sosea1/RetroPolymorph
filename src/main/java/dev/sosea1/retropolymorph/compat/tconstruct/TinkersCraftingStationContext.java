package dev.sosea1.retropolymorph.compat.tconstruct;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.SelectionScope;
import dev.sosea1.retropolymorph.core.CraftingContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Crafting context for Tinkers' persistent Crafting Station.
 *
 * <p>Besides invalidating Tinkers' FastWorkbench-style cache, this mirrors one
 * recipe choice across every live container backed by the same persistent
 * station inventory. The shared state is runtime-only and disappears when the
 * last viewer closes the station.</p>
 */
public class TinkersCraftingStationContext extends CraftingContext {

    public TinkersCraftingStationContext(
            Container container,
            InventoryCrafting matrix,
            Slot resultSlot) {
        super(container, matrix, resultSlot);
        TinkersSharedSelectionRegistry.bind(container, matrix);
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        List<IRecipe> matches = super.findAllMatches(world);
        if (TinkersSharedSelectionRegistry.reconcile(
                getContainer(), getRecipeMatrix(), matches)) {
            TinkersSharedSelectionRegistry.refreshAll(getContainer());
        }
        return matches;
    }

    @Override
    public boolean select(String recipeKey, World world) {
        invalidateCachedStationRecipe();
        boolean selected = super.select(recipeKey, world);
        if (!selected) {
            return false;
        }

        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId != null) {
            TinkersSharedSelectionRegistry.select(
                    getContainer(), getRecipeMatrix(), recipeId);
            TinkersSharedSelectionRegistry.refreshPeers(getContainer());
        }
        return true;
    }

    @Override
    public void clearSelection() {
        invalidateCachedStationRecipe();
        super.clearSelection();
        TinkersSharedSelectionRegistry.clear(getContainer(), getRecipeMatrix());
        TinkersSharedSelectionRegistry.refreshPeers(getContainer());
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation selected = TinkersSharedSelectionRegistry.selected(
                getContainer(), getRecipeMatrix());
        return selected == null ? null : selected.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        invalidateCachedStationRecipe();
        super.applyRemoteSelection(recipeKey);

        if (recipeKey == null) {
            TinkersSharedSelectionRegistry.clear(getContainer(), getRecipeMatrix());
            return;
        }
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId != null) {
            TinkersSharedSelectionRegistry.select(
                    getContainer(), getRecipeMatrix(), recipeId);
        }
    }

    @Override
    public SelectionScope getSelectionScope() {
        IInventory key = TinkersSharedSelectionRegistry.sharedKey(getRecipeMatrix());
        return key != null ? SelectionScope.shared(key) : SelectionScope.local();
    }

    private void invalidateCachedStationRecipe() {
        Container container = getContainer();
        if (container instanceof TinkersCraftingStationAccess) {
            ((TinkersCraftingStationAccess) container).retropolymorph$clearLastRecipe();
        }
    }
}
