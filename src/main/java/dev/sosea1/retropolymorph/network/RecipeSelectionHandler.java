package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

/**
 * Server-authoritative packet handler. No game state is touched on Netty's
 * network thread.
 */
public final class RecipeSelectionHandler
        implements IMessageHandler<RecipeSelectionMessage, IMessage> {

    @Override
    @Nullable
    public IMessage onMessage(final RecipeSelectionMessage message, MessageContext context) {
        if (!message.isValid()) {
            return null;
        }

        final EntityPlayerMP player = context.getServerHandler().player;
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
        Container container = player.openContainer;
        if (container == null || container.windowId != requestedWindowId) {
            NetworkHandler.sync(player, requestedWindowId, sessionToken, false, null);
            return;
        }

        if (!container.canInteractWith(player)) {
            NetworkHandler.sync(player, requestedWindowId, sessionToken, false, null);
            return;
        }

        SelectionContext selection = SelectionContextDetector.detect(container);
        if (selection == null) {
            NetworkHandler.sync(player, requestedWindowId, sessionToken, false, null);
            return;
        }

        if (message.isQuery()) {
            syncSelection(
                    player, requestedWindowId, sessionToken, true, selection);
            return;
        }

        if (message.isClear()) {
            selection.clearSelection();
            NetworkHandler.sync(player, requestedWindowId, sessionToken, true, null);
            return;
        }

        if (!message.isSelect()) {
            syncSelection(
                    player, requestedWindowId, sessionToken, false, selection);
            return;
        }

        String recipeKey = message.getRecipeKey();
        if (recipeKey == null) {
            syncSelection(
                    player, requestedWindowId, sessionToken, false, selection);
            return;
        }

        boolean accepted = selection.select(recipeKey, player.world);
        syncSelection(player, requestedWindowId, sessionToken, accepted, selection);
    }

    private static void syncSelection(
            EntityPlayerMP player,
            int windowId,
            int sessionToken,
            boolean accepted,
            SelectionContext selection) {
        String selected = selection.getSelectedRecipeKey();
        if (selected != null && !RecipeKey.isWireSafe(selected)) {
            selection.clearSelection();
            NetworkHandler.sync(player, windowId, sessionToken, false, null);
            return;
        }
        NetworkHandler.sync(player, windowId, sessionToken, accepted, selected);
    }
}
