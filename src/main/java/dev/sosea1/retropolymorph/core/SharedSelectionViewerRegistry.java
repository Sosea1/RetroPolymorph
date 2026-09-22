package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.Container;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime-only registry tracking active players and windows viewing a shared selection owner.
 *
 * <p>Keyed by owner identity (e.g. parent inventory of a crafting station).
 * Entries are cleaned up when containers close, players log out, or stale windows are detected.</p>
 */
public final class SharedSelectionViewerRegistry {

    public static final class ViewerEntry {
        private final UUID playerId;
        private final int windowId;

        public ViewerEntry(UUID playerId, int windowId) {
            if (playerId == null) {
                throw new NullPointerException("playerId");
            }
            this.playerId = playerId;
            this.windowId = windowId;
        }

        public UUID getPlayerId() {
            return this.playerId;
        }

        public int getWindowId() {
            return this.windowId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ViewerEntry that = (ViewerEntry) o;
            return this.windowId == that.windowId && this.playerId.equals(that.playerId);
        }

        @Override
        public int hashCode() {
            return 31 * this.playerId.hashCode() + this.windowId;
        }

        @Override
        public String toString() {
            return "ViewerEntry{" +
                    "playerId=" + this.playerId +
                    ", windowId=" + this.windowId +
                    '}';
        }
    }

    private static final Map<Object, Set<ViewerEntry>> OWNER_VIEWERS =
            new IdentityHashMap<Object, Set<ViewerEntry>>();
    private static final Map<UUID, Map<Integer, Object>> PLAYER_WINDOWS =
            new HashMap<UUID, Map<Integer, Object>>();

    private SharedSelectionViewerRegistry() {
    }

    /**
     * Registers or updates a viewer for a shared owner.
     */
    public static synchronized void register(
            Object ownerIdentity,
            UUID playerId,
            int windowId) {
        if (ownerIdentity == null || playerId == null) {
            return;
        }

        Map<Integer, Object> playerMap = PLAYER_WINDOWS.get(playerId);
        if (playerMap == null) {
            playerMap = new HashMap<Integer, Object>();
            PLAYER_WINDOWS.put(playerId, playerMap);
        }

        Object previousOwner = playerMap.put(Integer.valueOf(windowId), ownerIdentity);
        if (previousOwner != null && previousOwner != ownerIdentity) {
            removeViewer(previousOwner, playerId, windowId);
        }

        Set<ViewerEntry> viewers = OWNER_VIEWERS.get(ownerIdentity);
        if (viewers == null) {
            viewers = new HashSet<ViewerEntry>();
            OWNER_VIEWERS.put(ownerIdentity, viewers);
        }

        ViewerEntry entry = new ViewerEntry(playerId, windowId);
        viewers.remove(entry);
        viewers.add(entry);
    }

    /**
     * Resets all shared viewer mappings across all owners and players.
     * Invoked during world unloads or server stopping to prevent cross-session retention.
     */
    public static synchronized void reset() {
        OWNER_VIEWERS.clear();
        PLAYER_WINDOWS.clear();
    }

    public static synchronized int getTrackedOwnerCount() {
        return OWNER_VIEWERS.size();
    }

    /**
     * Unregisters a specific window for a player.
     */
    public static synchronized void unregister(UUID playerId, int windowId) {
        if (playerId == null) {
            return;
        }

        Map<Integer, Object> playerMap = PLAYER_WINDOWS.get(playerId);
        if (playerMap == null) {
            return;
        }

        Object owner = playerMap.remove(Integer.valueOf(windowId));
        if (playerMap.isEmpty()) {
            PLAYER_WINDOWS.remove(playerId);
        }

        if (owner != null) {
            removeViewer(owner, playerId, windowId);
        }
    }

    /**
     * Unregisters all active windows for a player (e.g. on logout).
     */
    public static synchronized void unregisterPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }

        Map<Integer, Object> playerMap = PLAYER_WINDOWS.remove(playerId);
        if (playerMap == null) {
            return;
        }

        for (Map.Entry<Integer, Object> entry : playerMap.entrySet()) {
            removeViewer(entry.getValue(), playerId, entry.getKey().intValue());
        }
    }

    /**
     * Returns an unmodifiable snapshot of active viewers for the given shared owner identity.
     */
    public static synchronized List<ViewerEntry> getViewers(Object ownerIdentity) {
        if (ownerIdentity == null) {
            return Collections.emptyList();
        }

        Set<ViewerEntry> viewers = OWNER_VIEWERS.get(ownerIdentity);
        if (viewers == null || viewers.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(new ArrayList<ViewerEntry>(viewers));
    }

    /**
     * Resets all tracked viewers (for tests).
     */
    public static synchronized void clear() {
        OWNER_VIEWERS.clear();
        PLAYER_WINDOWS.clear();
    }

    private static void removeViewer(Object owner, UUID playerId, int windowId) {
        Set<ViewerEntry> viewers = OWNER_VIEWERS.get(owner);
        if (viewers == null) {
            return;
        }

        viewers.remove(new ViewerEntry(playerId, windowId));
        if (viewers.isEmpty()) {
            OWNER_VIEWERS.remove(owner);
        }
    }
}
