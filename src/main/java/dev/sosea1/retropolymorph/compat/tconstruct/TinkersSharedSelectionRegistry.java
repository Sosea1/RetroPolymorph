package dev.sosea1.retropolymorph.compat.tconstruct;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime-only shared recipe selection for Tinkers' persistent Crafting Station.
 *
 * <p>Tinkers gives each viewer a distinct InventoryCraftingPersistent wrapper,
 * while all wrappers delegate to the same parent tile inventory. Vanilla-style
 * matrix-local state would therefore allow two players looking at the same
 * station to disagree about the selected result. This registry keys the choice
 * by that shared parent identity and mirrors it into every live matrix wrapper.
 * Entries disappear when the last known container closes; nothing is written to
 * tile NBT or world saves.</p>
 */
public final class TinkersSharedSelectionRegistry {

    private static final Map<IInventory, StationState> STATIONS =
            new IdentityHashMap<IInventory, StationState>();
    private static final Map<Container, IInventory> CONTAINER_KEYS =
            new IdentityHashMap<Container, IInventory>();

    private TinkersSharedSelectionRegistry() {
    }

    /**
     * Resets all shared Tinkers station selections.
     * Invoked during world unloads or server stopping to prevent cross-session retention.
     */
    public static synchronized void reset() {
        STATIONS.clear();
        CONTAINER_KEYS.clear();
    }

    public static synchronized int getBoundStationCount() {
        return STATIONS.size();
    }

    /** Idempotently attaches a live container and inherits the station choice. */
    public static synchronized void bind(Container container, InventoryCrafting matrix) {
        IInventory key = sharedKey(matrix);
        if (container == null || key == null) {
            return;
        }

        IInventory previousKey = CONTAINER_KEYS.get(container);
        if (previousKey != null && previousKey != key) {
            detach(container, previousKey);
        }

        CONTAINER_KEYS.put(container, key);
        StationState station = STATIONS.get(key);
        if (station == null) {
            station = new StationState();
            STATIONS.put(key, station);
        }
        station.matrices.put(container, matrix);

        if (station.selectedRecipeId != null) {
            setLocalSelection(matrix, station.selectedRecipeId);
        } else {
            ResourceLocation local = localSelection(matrix);
            if (local != null) {
                station.selectedRecipeId = local;
                mirrorSelection(station, local);
            }
        }
    }

    public static synchronized void select(
            Container container,
            InventoryCrafting matrix,
            ResourceLocation recipeId) {
        if (recipeId == null) {
            clear(container, matrix);
            return;
        }
        bind(container, matrix);
        StationState station = stationFor(container);
        if (station == null) {
            return;
        }
        station.selectedRecipeId = recipeId;
        mirrorSelection(station, recipeId);
    }

    public static synchronized void clear(Container container, InventoryCrafting matrix) {
        bind(container, matrix);
        StationState station = stationFor(container);
        if (station == null) {
            clearLocalSelection(matrix);
            return;
        }
        station.selectedRecipeId = null;
        for (InventoryCrafting liveMatrix : station.matrices.values()) {
            clearLocalSelection(liveMatrix);
        }
    }

    /**
     * Drops a stale shared choice when it no longer belongs to the live match set.
     * Called after recipe discovery, so transient SlotCrafting mutations are not
     * mistaken for a real conflict change.
     *
     * @return true when a stale shared selection was cleared
     */
    public static synchronized boolean reconcile(
            Container container,
            InventoryCrafting matrix,
            List<IRecipe> matches) {
        bind(container, matrix);
        StationState station = stationFor(container);
        if (station == null || station.selectedRecipeId == null) {
            return false;
        }

        ResourceLocation selected = station.selectedRecipeId;
        if (contains(matches, selected)) {
            mirrorSelection(station, selected);
            return false;
        }

        station.selectedRecipeId = null;
        for (InventoryCrafting liveMatrix : station.matrices.values()) {
            clearLocalSelection(liveMatrix);
        }
        return true;
    }

    @Nullable
    public static synchronized ResourceLocation selected(
            Container container,
            InventoryCrafting matrix) {
        bind(container, matrix);
        StationState station = stationFor(container);
        return station == null ? localSelection(matrix) : station.selectedRecipeId;
    }

    /** Recomputes all other open station previews after a shared choice changed. */
    public static void refreshPeers(Container source) {
        refresh(snapshotRefreshTargets(source, false));
    }

    /** Refreshes every bound preview, including the source. Used after reconciliation. */
    public static void refreshAll(Container source) {
        refresh(snapshotRefreshTargets(source, true));
    }

    /**
     * Copies refresh targets while holding the registry monitor, then releases it
     * before invoking any foreign Container callbacks. Container implementations
     * may synchronously re-enter crafting/selector code, so calling them while the
     * registry lock is held creates an avoidable cross-thread deadlock boundary.
     */
    private static synchronized List<RefreshTarget> snapshotRefreshTargets(
            Container source,
            boolean includeSource) {
        StationState station = stationFor(source);
        if (station == null || station.matrices.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<RefreshTarget> targets = new ArrayList<RefreshTarget>(station.matrices.size());
        for (Map.Entry<Container, InventoryCrafting> entry : station.matrices.entrySet()) {
            if (!includeSource && entry.getKey() == source) {
                continue;
            }
            targets.add(new RefreshTarget(entry.getKey(), entry.getValue()));
        }
        return targets;
    }

    private static void refresh(List<RefreshTarget> targets) {
        for (RefreshTarget target : targets) {
            invalidate(target.container);
            target.container.onCraftMatrixChanged(target.matrix);
            target.container.detectAndSendChanges();
        }
    }

    public static synchronized boolean sameStation(Container first, Container second) {
        if (first == null || second == null) {
            return false;
        }
        IInventory firstKey = CONTAINER_KEYS.get(first);
        IInventory secondKey = CONTAINER_KEYS.get(second);
        return firstKey != null && firstKey == secondKey;
    }

    public static synchronized void onContainerClosed(Container container) {
        if (container == null) {
            return;
        }
        IInventory key = CONTAINER_KEYS.remove(container);
        if (key != null) {
            detach(container, key);
        }
    }

    /** Test/diagnostic visibility without exposing mutable station state. */
    public static synchronized int activeStationCount() {
        return STATIONS.size();
    }

    private static void detach(Container container, IInventory key) {
        StationState station = STATIONS.get(key);
        if (station == null) {
            return;
        }
        station.matrices.remove(container);
        if (station.matrices.isEmpty()) {
            STATIONS.remove(key);
        }
    }

    @Nullable
    private static StationState stationFor(Container container) {
        IInventory key = CONTAINER_KEYS.get(container);
        return key == null ? null : STATIONS.get(key);
    }

    @Nullable
    public static IInventory sharedKey(InventoryCrafting matrix) {
        if (!(matrix instanceof TinkersPersistentMatrixAccess)) {
            return null;
        }
        IInventory parent = ((TinkersPersistentMatrixAccess) matrix)
                .retropolymorph$getPersistentParent();
        return parent;
    }

    private static void mirrorSelection(StationState station, ResourceLocation recipeId) {
        for (InventoryCrafting matrix : station.matrices.values()) {
            setLocalSelection(matrix, recipeId);
        }
    }

    private static void setLocalSelection(InventoryCrafting matrix, ResourceLocation recipeId) {
        if (!(matrix instanceof CraftingMatrixExtension)) {
            return;
        }
        ((CraftingMatrixExtension) matrix)
                .retropolymorph$getOrCreateRecipeSelectionState()
                .select(recipeId);
    }

    private static void clearLocalSelection(InventoryCrafting matrix) {
        if (!(matrix instanceof CraftingMatrixExtension)) {
            return;
        }
        RecipeSelectionState state = ((CraftingMatrixExtension) matrix)
                .retropolymorph$peekRecipeSelectionState();
        if (state != null) {
            state.clear();
        }
    }

    @Nullable
    private static ResourceLocation localSelection(InventoryCrafting matrix) {
        if (!(matrix instanceof CraftingMatrixExtension)) {
            return null;
        }
        RecipeSelectionState state = ((CraftingMatrixExtension) matrix)
                .retropolymorph$peekRecipeSelectionState();
        return state == null ? null : state.getSelectedRecipeId();
    }

    private static boolean contains(List<IRecipe> matches, ResourceLocation selected) {
        if (matches == null || selected == null) {
            return false;
        }
        for (IRecipe recipe : matches) {
            if (recipe != null && selected.equals(recipe.getRegistryName())) {
                return true;
            }
        }
        return false;
    }

    private static void invalidate(Container container) {
        if (container instanceof TinkersCraftingStationAccess) {
            ((TinkersCraftingStationAccess) container).retropolymorph$clearLastRecipe();
        }
    }

    private static final class RefreshTarget {
        private final Container container;
        private final InventoryCrafting matrix;

        private RefreshTarget(Container container, InventoryCrafting matrix) {
            this.container = container;
            this.matrix = matrix;
        }
    }

    private static final class StationState {
        private final Map<Container, InventoryCrafting> matrices =
                new IdentityHashMap<Container, InventoryCrafting>();

        @Nullable
        private ResourceLocation selectedRecipeId;
    }
}
