package dev.sosea1.retropolymorph.compat.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Transient Arcane Workbench selection keyed by the shared tile matrix and player.
 *
 * TileArcaneWorkbench owns one persistent InventoryCrafting. Keeping the choice in
 * the matrix itself would make two players viewing the same workbench overwrite
 * each other. PlayerRecipePreferences remains the durable cross-GUI layer.
 */
final class ThaumcraftArcaneSelectionStore {

    private static final Map<InventoryCrafting, Map<UUID, Entry>> SELECTIONS =
            new WeakHashMap<InventoryCrafting, Map<UUID, Entry>>();

    private ThaumcraftArcaneSelectionStore() {
    }

    static synchronized void select(
            InventoryCrafting matrix,
            EntityPlayer player,
            ResourceLocation recipeId) {
        if (matrix == null || player == null || recipeId == null) {
            return;
        }
        Map<UUID, Entry> byPlayer = SELECTIONS.get(matrix);
        if (byPlayer == null) {
            byPlayer = new HashMap<UUID, Entry>();
            SELECTIONS.put(matrix, byPlayer);
        }
        byPlayer.put(player.getUniqueID(), new Entry(recipeId));
    }

    @Nullable
    static synchronized ResourceLocation get(
            InventoryCrafting matrix,
            EntityPlayer player) {
        Entry entry = getEntry(matrix, player);
        return entry == null ? null : entry.recipeId;
    }

    static synchronized void markObserved(
            InventoryCrafting matrix,
            EntityPlayer player,
            ResourceLocation recipeId) {
        Entry entry = getEntry(matrix, player);
        if (entry != null && entry.recipeId.equals(recipeId)) {
            entry.observed = true;
        }
    }

    static synchronized boolean wasObserved(
            InventoryCrafting matrix,
            EntityPlayer player,
            ResourceLocation recipeId) {
        Entry entry = getEntry(matrix, player);
        return entry != null && entry.recipeId.equals(recipeId) && entry.observed;
    }

    static synchronized void clear(
            InventoryCrafting matrix,
            EntityPlayer player) {
        if (matrix == null || player == null) {
            return;
        }
        Map<UUID, Entry> byPlayer = SELECTIONS.get(matrix);
        if (byPlayer == null) {
            return;
        }
        byPlayer.remove(player.getUniqueID());
        if (byPlayer.isEmpty()) {
            SELECTIONS.remove(matrix);
        }
    }

    @Nullable
    private static Entry getEntry(
            InventoryCrafting matrix,
            EntityPlayer player) {
        if (matrix == null || player == null) {
            return null;
        }
        Map<UUID, Entry> byPlayer = SELECTIONS.get(matrix);
        return byPlayer == null ? null : byPlayer.get(player.getUniqueID());
    }

    private static final class Entry {
        private final ResourceLocation recipeId;
        private boolean observed;

        private Entry(ResourceLocation recipeId) {
            this.recipeId = recipeId;
        }
    }
}
