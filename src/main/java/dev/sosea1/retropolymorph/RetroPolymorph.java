package dev.sosea1.retropolymorph;

import dev.sosea1.retropolymorph.command.CommandRetroPolymorph;
import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]"
)
public final class RetroPolymorph {

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            PolymorphConfig.load(event.getSuggestedConfigurationFile());
        }
        CompatibilityBootstrap.init();
        NetworkHandler.init();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandRetroPolymorph());
    }
}
