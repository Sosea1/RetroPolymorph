package dev.sosea1.retropolymorph.proxy;

import dev.sosea1.retropolymorph.network.RecipeSelectionHandler;
import dev.sosea1.retropolymorph.core.CraftingPreferenceSeeder;
import dev.sosea1.retropolymorph.compat.tconstruct.TinkersSharedSelectionRegistry;
import net.minecraft.inventory.Container;
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
        Container container = event.getContainer();
        TinkersSharedSelectionRegistry.onContainerClosed(container);
        CraftingPreferenceSeeder.onContainerClosed(container);
        if (dev.sosea1.retropolymorph.config.PolymorphConfig.isIntegrationAe2Enabled()) {
            dev.sosea1.retropolymorph.compat.ae2.Ae2CraftExecutionScope.onContainerClosed(container);
        }
        if (!dev.sosea1.retropolymorph.config.PolymorphConfig.isRememberPlayerChoices()) {
            if (dev.sosea1.retropolymorph.config.PolymorphConfig.isIntegrationAe2Enabled()) {
                dev.sosea1.retropolymorph.compat.ae2.Ae2SelectionStore.clear(container);
                dev.sosea1.retropolymorph.compat.ae2.Ae2TerminalRecipePin.clearSelection(container);
            }
            for (net.minecraft.inventory.Slot slot : container.inventorySlots) {
                if (slot != null && slot.inventory instanceof net.minecraft.inventory.InventoryCrafting) {
                    dev.sosea1.retropolymorph.core.RecipeSelectionSeeder.clear(
                            (net.minecraft.inventory.InventoryCrafting) slot.inventory);
                }
            }
        }
        if (!event.getEntityPlayer().world.isRemote) {
            RecipeSelectionHandler.onContainerClosed(
                    event.getEntityPlayer().getUniqueID(),
                    container.windowId);
        }
    }

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
            if (dev.sosea1.retropolymorph.config.PolymorphConfig.isIntegrationAe2Enabled()) {
                dev.sosea1.retropolymorph.compat.ae2.Ae2CraftExecutionScope.resetIfLeaked();
                dev.sosea1.retropolymorph.compat.ae2.Ae2MatrixChangeScope.resetIfLeaked();
            }
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
        CraftingPreferenceSeeder.reset();
        dev.sosea1.retropolymorph.core.SharedSelectionViewerRegistry.reset();
    }
}
