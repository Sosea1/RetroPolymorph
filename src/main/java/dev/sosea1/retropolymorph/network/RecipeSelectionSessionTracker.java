package dev.sosea1.retropolymorph.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Last live selector session observed from each player.
 *
 * <p>The server needs this only for peer updates of genuinely shared crafting
 * surfaces (notably Tinkers' Crafting Station). Reusing the client's session
 * token and input revision lets the normal client stale-packet checks remain in
 * force for unsolicited peer syncs.</p>
 */
final class RecipeSelectionSessionTracker {

    private final ConcurrentHashMap<UUID, Session> sessions =
            new ConcurrentHashMap<UUID, Session>();

    void observe(UUID playerId, int windowId, int sessionToken, int inputRevision) {
        if (playerId == null || windowId < 0 || sessionToken == 0) {
            return;
        }
        sessions.put(playerId, new Session(windowId, sessionToken, inputRevision));
    }

    Session get(UUID playerId, int expectedWindowId) {
        if (playerId == null) {
            return null;
        }
        Session session = sessions.get(playerId);
        return session != null && session.windowId == expectedWindowId ? session : null;
    }

    void clear(UUID playerId) {
        if (playerId != null) {
            sessions.remove(playerId);
        }
    }

    void clearWindow(UUID playerId, int windowId) {
        if (playerId == null) {
            return;
        }
        Session current = sessions.get(playerId);
        if (current != null && current.windowId == windowId) {
            sessions.remove(playerId, current);
        }
    }

    static final class Session {
        final int windowId;
        final int sessionToken;
        final int inputRevision;

        Session(int windowId, int sessionToken, int inputRevision) {
            this.windowId = windowId;
            this.sessionToken = sessionToken;
            this.inputRevision = inputRevision;
        }
    }
}
