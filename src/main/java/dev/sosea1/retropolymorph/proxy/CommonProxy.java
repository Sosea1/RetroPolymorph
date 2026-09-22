package dev.sosea1.retropolymorph.proxy;

import dev.sosea1.retropolymorph.network.RecipeSelectionHandler;
import dev.sosea1.retropolymorph.compat.tconstruct.TinkersSharedSelectionRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** Common-side lifecycle bridge. Kept free of client-only class references. */
public class CommonProxy {

    public void preInit() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event != null && event.player != null) {
            RecipeSelectionHandler.onPlayerLoggedOut(event.player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event == null || event.getContainer() == null || event.getEntityPlayer() == null) {
            return;
        }
        TinkersSharedSelectionRegistry.onContainerClosed(event.getContainer());
        if (!event.getEntityPlayer().world.isRemote) {
            RecipeSelectionHandler.onContainerClosed(
                    event.getEntityPlayer().getUniqueID(),
                    event.getContainer().windowId);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event != null && event.getWorld() != null && !event.getWorld().isRemote) {
            if (event.getWorld().provider.getDimension() == 0) {
                onServerStopping();
            }
        }
    }

    public void onServerStopping() {
        TinkersSharedSelectionRegistry.reset();
        dev.sosea1.retropolymorph.core.SharedSelectionViewerRegistry.reset();
    }
}
