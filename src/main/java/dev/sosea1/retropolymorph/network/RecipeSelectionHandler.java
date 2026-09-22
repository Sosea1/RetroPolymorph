package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeOptions;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.api.SelectionScope;
import dev.sosea1.retropolymorph.core.SelectionCommand;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import dev.sosea1.retropolymorph.core.SelectionContextGuard;
import dev.sosea1.retropolymorph.core.SelectionService;
import dev.sosea1.retropolymorph.core.SelectionServiceResult;
import dev.sosea1.retropolymorph.core.SharedSelectionViewerRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative packet handler. No game state is touched on Netty's thread. */
public final class RecipeSelectionHandler
        implements IMessageHandler<RecipeSelectionMessage, IMessage> {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final ConcurrentHashMap<UUID, RecipeSelectionRateLimiter> RATE_LIMITS =
            new ConcurrentHashMap<UUID, RecipeSelectionRateLimiter>();
    private static final RecipeSelectionSessionTracker SESSIONS =
            new RecipeSelectionSessionTracker();

    public static void onPlayerLoggedOut(UUID playerId) {
        if (playerId != null) {
            RATE_LIMITS.remove(playerId);
            SESSIONS.clear(playerId);
            SharedSelectionViewerRegistry.unregisterPlayer(playerId);
        }
    }

    @Override
    @Nullable
    public IMessage onMessage(final RecipeSelectionMessage message, MessageContext context) {
        if (!message.isValid()) {
            return null;
        }

        final EntityPlayerMP player = context.getServerHandler().player;
        RecipeSelectionRateLimiter limiter = RATE_LIMITS.get(player.getUniqueID());
        if (limiter == null) {
            RecipeSelectionRateLimiter created = new RecipeSelectionRateLimiter();
            RecipeSelectionRateLimiter existing = RATE_LIMITS.putIfAbsent(
                    player.getUniqueID(), created);
            limiter = existing != null ? existing : created;
        }
        if (!limiter.allow(message.isQuery())) {
            LOGGER.debug(
                    "Throttled excess C2S selector packet from player={}, type={}",
                    player.getName(),
                    message.isQuery() ? "query" : (message.isSelect() ? "select" : "clear"));
            return null;
        }

        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                handleOnServerThread(player, message);
            }
        });
        return null;
    }

    private static void handleOnServerThread(
            EntityPlayerMP player,
            RecipeSelectionMessage message) {
        int requestedWindowId = message.getWindowId();
        int sessionToken = message.getSessionToken();
        int inputRevision = message.getInputRevision();
        Container container = player.openContainer;
        if (container == null || container.windowId != requestedWindowId) {
            NetworkHandler.sync(
                    player, requestedWindowId, sessionToken, inputRevision,
                    false, null, Collections.<RecipeOption>emptyList());
            return;
        }

        if (!container.canInteractWith(player)) {
            NetworkHandler.sync(
                    player, requestedWindowId, sessionToken, inputRevision,
                    false, null, Collections.<RecipeOption>emptyList());
            return;
        }

        SESSIONS.observe(
                player.getUniqueID(), requestedWindowId, sessionToken, inputRevision);

        SelectionContext selection = SelectionContextDetector.detect(container);
        if (selection == null) {
            if (message.isQuery()) {
                LOGGER.debug(
                        "Server selector query unsupported: player={}, container={}, window={}",
                        player.getName(),
                        container.getClass().getName(),
                        Integer.valueOf(requestedWindowId));
            }
            NetworkHandler.sync(
                    player, requestedWindowId, sessionToken, inputRevision,
                    false, null, Collections.<RecipeOption>emptyList());
            return;
        }

        SelectionScope scope = selection.getSelectionScope();
        if (scope.isShared()) {
            SharedSelectionViewerRegistry.register(
                    scope.getOwnerIdentity(), player.getUniqueID(), requestedWindowId);
        }

        SelectionCommand command;
        if (message.isClear()) {
            command = SelectionCommand.clear();
        } else if (message.isSelect()) {
            command = SelectionCommand.select(message.getRecipeKey());
        } else {
            command = SelectionCommand.query();
        }

        SelectionServiceResult result = SelectionService.handle(player, selection, command);

        if (message.isQuery()) {
            LOGGER.debug(
                    "Server selector query: player={}, container={}, context={}, window={}, options={}, reason={}",
                    player.getName(),
                    container.getClass().getName(),
                    selection.getClass().getName(),
                    Integer.valueOf(requestedWindowId),
                    Integer.valueOf(result.getOptions().size()),
                    result.getReason());
        }

        if (result.isSelectionChanged()) {
            SelectionPeerSyncService.syncPeers(player, selection);
        }

        syncSelection(
                player,
                requestedWindowId,
                sessionToken,
                inputRevision,
                result.isAccepted(),
                result.getSelectedRecipeKey(),
                result.getOptions(),
                result.getReason());
    }

    public static void onContainerClosed(UUID playerId, int windowId) {
        SESSIONS.clearWindow(playerId, windowId);
        SharedSelectionViewerRegistry.unregister(playerId, windowId);
    }

    static RecipeSelectionSessionTracker.Session getSession(UUID playerId, int windowId) {
        return SESSIONS.get(playerId, windowId);
    }

    static void syncSelection(
            EntityPlayerMP player,
            int windowId,
            int sessionToken,
            int inputRevision,
            boolean accepted,
            @Nullable String selected,
            List<RecipeOption> options,
            SelectionReason reason) {
        List<RecipeOption> visible = RecipeOptions.sanitizeAndLimit(options, selected);
        LOGGER.debug(
                "Server selector sync: player={}, window={}, session={}, revision={}, selected={}, accepted={}, reason={}, matches={}",
                player.getName(),
                Integer.valueOf(windowId),
                Integer.valueOf(sessionToken),
                Integer.valueOf(inputRevision),
                selected,
                Boolean.valueOf(accepted),
                reason,
                Integer.valueOf(options.size()));
        NetworkHandler.sync(
                player,
                windowId,
                sessionToken,
                inputRevision,
                accepted,
                selected,
                visible,
                reason);
    }

    static void syncSelection(
            EntityPlayerMP player,
            int windowId,
            int sessionToken,
            int inputRevision,
            boolean accepted,
            @Nullable String selected,
            List<RecipeOption> options) {
        syncSelection(player, windowId, sessionToken, inputRevision, accepted, selected, options, SelectionReason.NATIVE_DEFAULT);
    }
}
