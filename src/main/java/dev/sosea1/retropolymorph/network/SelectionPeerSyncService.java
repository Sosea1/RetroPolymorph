package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.api.SelectionScope;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import dev.sosea1.retropolymorph.core.SelectionContextGuard;
import dev.sosea1.retropolymorph.core.SharedSelectionViewerRegistry;
import dev.sosea1.retropolymorph.core.SharedSelectionViewerRegistry.ViewerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pushes updated shared selection state only to registered peer viewers of the same shared owner.
 *
 * <p>Avoids scanning all online server players.</p>
 */
public final class SelectionPeerSyncService {

    private SelectionPeerSyncService() {
    }

    /**
     * Pushes an updated shared selection to all other live viewers of the same owner,
     * preserving each peer's own window ID, session token, and input revision.
     */
    public static void syncPeers(
            EntityPlayerMP sourcePlayer,
            SelectionContext sourceSelection) {
        if (sourcePlayer == null || sourceSelection == null) {
            return;
        }

        SelectionScope scope = sourceSelection.getSelectionScope();
        if (!scope.isShared()) {
            return;
        }

        Object owner = scope.getOwnerIdentity();
        if (owner == null) {
            return;
        }

        List<ViewerEntry> viewers = SharedSelectionViewerRegistry.getViewers(owner);
        if (viewers.isEmpty()) {
            return;
        }

        UUID sourceId = sourcePlayer.getUniqueID();
        for (ViewerEntry entry : viewers) {
            if (entry.getPlayerId().equals(sourceId)) {
                continue;
            }

            EntityPlayerMP peer = sourcePlayer.getServer().getPlayerList().getPlayerByUUID(entry.getPlayerId());
            if (peer == null || peer.openContainer == null || peer.openContainer.windowId != entry.getWindowId()) {
                SharedSelectionViewerRegistry.unregister(entry.getPlayerId(), entry.getWindowId());
                continue;
            }

            Container peerContainer = peer.openContainer;
            RecipeSelectionSessionTracker.Session session = RecipeSelectionHandler.getSession(
                    peer.getUniqueID(), peerContainer.windowId);
            if (session == null) {
                continue;
            }

            SelectionContext peerSelection = SelectionContextDetector.detect(peerContainer);
            if (peerSelection == null
                    || !peerSelection.getSelectionScope().isShared()
                    || owner != peerSelection.getSelectionScope().getOwnerIdentity()) {
                SharedSelectionViewerRegistry.unregister(entry.getPlayerId(), entry.getWindowId());
                continue;
            }

            List<RecipeOption> peerOptions = SelectionContextGuard.findOptions(
                    peerSelection, peer.world);
            if (peerOptions == null) {
                continue;
            }

            RecipeSelectionHandler.syncSelection(
                    peer,
                    session.windowId,
                    session.sessionToken,
                    session.inputRevision,
                    true,
                    SelectionContextGuard.selected(peerSelection),
                    peerOptions,
                    SelectionReason.CURRENT_CONTEXT);
        }
    }
}
