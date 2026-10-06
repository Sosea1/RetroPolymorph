package com.sosea1.retropolymorph;

import com.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import com.sosea1.retropolymorph.network.NetworkHandler;
import com.sosea1.retropolymorph.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:mixinbooter@[11.0,);"
)
public final class RetroPolymorph {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    @SidedProxy(
            clientSide = "com.sosea1.retropolymorph.proxy.ClientProxy",
            serverSide = "com.sosea1.retropolymorph.proxy.CommonProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        PolymorphConfig.load(event.getSuggestedConfigurationFile());
        PROXY.preInit();
        CompatibilityBootstrap.init();
        NetworkHandler.init();
        LOGGER.info(
                "PreInit complete: side={}, selectorEnabled={}, selectorMode={}",
                event.getSide(),
                Boolean.valueOf(PolymorphConfig.isSelectorEnabled()),
                PolymorphConfig.getSelectorMode());
    }

    @Mod.EventHandler
    public void onServerStopping(net.minecraftforge.fml.common.event.FMLServerStoppingEvent event) {
        PROXY.onServerStopping();
    }
}
