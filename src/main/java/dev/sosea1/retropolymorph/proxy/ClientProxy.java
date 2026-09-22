package dev.sosea1.retropolymorph.proxy;

import dev.sosea1.retropolymorph.client.ClientGuiEvents;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Explicit client event registration avoids relying on automatic subscriber discovery. */
public final class ClientProxy extends CommonProxy {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static boolean initialized;

    @Override
    public void preInit() {
        if (initialized) {
            return;
        }
        super.preInit();
        MinecraftForge.EVENT_BUS.register(new ClientGuiEvents());
        initialized = true;
        LOGGER.info("Client GUI event bridge registered explicitly");
    }
}
