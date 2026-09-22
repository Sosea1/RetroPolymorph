package dev.sosea1.retropolymorph.compat.extrautils2;

import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class ExtraUtils2Integration implements IntegrationRegistrar {

    public static final ExtraUtils2Integration INSTANCE = new ExtraUtils2Integration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "extrautils2",
            "Extra Utilities 2",
            CompatibilityBootstrap.PRIORITY_VIRTUAL_MACHINE);

    private ExtraUtils2Integration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationExtraUtilities2Enabled();
    }

    @Override
    public void registerEnabled() {
        registerMachineAdapter(
                new ResourceLocation("retropolymorph", "extrautils2_crafters"),
                descriptor().getDefaultPriority(),
                ExtraUtilities2CrafterAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "extrautils2_crafter_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "com.rwtema.extrautils2.tile.TileCrafter$CrafterContainer"));
        registerAdapter(
                new ResourceLocation("retropolymorph", "extrautils2_analog_crafter_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "com.rwtema.extrautils2.tile.TileAnalogCrafter$ContainerAnalogCrafter"));
    }
}
