package com.sosea1.retropolymorph.network;

import com.sosea1.retropolymorph.Tags;
import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.api.RecipeOption;
import com.sosea1.retropolymorph.api.RecipeOptions;
import com.sosea1.retropolymorph.api.SelectionReason;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.List;

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
        CHANNEL.registerMessage(RFToolsEditorMessage.Handler.class, RFToolsEditorMessage.class, 2, Side.SERVER);
    }

    public static void select(
            int windowId,
            int sessionToken,
            int inputRevision,
            String recipeKey) {
        CHANNEL.sendToServer(RecipeSelectionMessage.select(
                windowId, sessionToken, inputRevision, recipeKey));
    }

    public static void clear(int windowId, int sessionToken, int inputRevision) {
        CHANNEL.sendToServer(RecipeSelectionMessage.clear(
                windowId, sessionToken, inputRevision));
    }

    public static void query(int windowId, int sessionToken, int inputRevision) {
        CHANNEL.sendToServer(RecipeSelectionMessage.query(
                windowId, sessionToken, inputRevision));
    }

    public static void openRFToolsTemplate(int windowId, int recipeIndex) {
        CHANNEL.sendToServer(new RFToolsEditorMessage(windowId, recipeIndex));
    }

    static void sync(
            EntityPlayerMP player,
            int windowId,
            int sessionToken,
            int inputRevision,
            boolean accepted,
            @Nullable String selectedRecipeKey,
            List<RecipeOption> options,
            SelectionReason reason) {
        boolean safe = selectedRecipeKey == null || RecipeKey.isWireSafe(selectedRecipeKey);
        List<RecipeOption> safeOptions = RecipeOptions.sanitizeAndLimit(
                options,
                safe ? selectedRecipeKey : null);
        CHANNEL.sendTo(
                new RecipeSelectionSyncMessage(
                        windowId,
                        sessionToken,
                        inputRevision,
                        accepted && safe,
                        safe ? selectedRecipeKey : null,
                        safeOptions,
                        reason),
                player);
    }

    static void sync(
            EntityPlayerMP player,
            int windowId,
            int sessionToken,
            int inputRevision,
            boolean accepted,
            @Nullable String selectedRecipeKey,
            List<RecipeOption> options) {
        sync(player, windowId, sessionToken, inputRevision, accepted, selectedRecipeKey, options, SelectionReason.NATIVE_DEFAULT);
    }
}
