package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.Tags;
import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

/**
 * Minimal bidirectional networking. Recipe keys are opaque to this layer; the
 * active server context validates and interprets them.
 */
public final class NetworkHandler {

    private static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private static boolean initialized;

    private NetworkHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        CHANNEL.registerMessage(
                RecipeSelectionHandler.class,
                RecipeSelectionMessage.class,
                0,
                Side.SERVER);
        CHANNEL.registerMessage(
                RecipeSelectionSyncHandler.class,
                RecipeSelectionSyncMessage.class,
                1,
                Side.CLIENT);
        initialized = true;
    }

    public static void select(int windowId, int sessionToken, String recipeKey) {
        CHANNEL.sendToServer(RecipeSelectionMessage.select(windowId, sessionToken, recipeKey));
    }

    public static void clear(int windowId, int sessionToken) {
        CHANNEL.sendToServer(RecipeSelectionMessage.clear(windowId, sessionToken));
    }

    public static void query(int windowId, int sessionToken) {
        CHANNEL.sendToServer(RecipeSelectionMessage.query(windowId, sessionToken));
    }

    static void sync(
            EntityPlayerMP player,
            int windowId,
            int sessionToken,
            boolean accepted,
            @Nullable String selectedRecipeKey) {
        boolean safe = selectedRecipeKey == null || RecipeKey.isWireSafe(selectedRecipeKey);
        CHANNEL.sendTo(
                new RecipeSelectionSyncMessage(
                        windowId,
                        sessionToken,
                        accepted && safe,
                        safe ? selectedRecipeKey : null),
                player);
    }
}
