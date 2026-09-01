package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.client.ClientSelectionTracker;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

/**
 * Client acknowledgement handler. Scheduling goes through Forge's common
 * IThreadListener lookup so this class does not reference Minecraft's client
 * implementation directly.
 */
public final class RecipeSelectionSyncHandler
        implements IMessageHandler<RecipeSelectionSyncMessage, IMessage> {

    @Override
    @Nullable
    public IMessage onMessage(final RecipeSelectionSyncMessage message, MessageContext context) {
        if (!message.isValid()) {
            return null;
        }

        final String selected = message.getSelectedRecipeKey();
        IThreadListener thread = FMLCommonHandler.instance().getWorldThread(context.netHandler);
        thread.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientSelectionTracker.apply(
                        message.getWindowId(),
                        message.getSessionToken(),
                        message.isAccepted(),
                        selected);
            }
        });
        return null;
    }
}
