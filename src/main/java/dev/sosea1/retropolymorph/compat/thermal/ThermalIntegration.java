package dev.sosea1.retropolymorph.compat.thermal;

import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class ThermalIntegration implements IntegrationRegistrar {

    public static final ThermalIntegration INSTANCE = new ThermalIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "thermalexpansion",
            "Thermal Expansion",
            CompatibilityBootstrap.PRIORITY_THERMAL);

    private ThermalIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationThermalEnabled();
    }

    @Override
    public void registerEnabled() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "thermal_sequential_fabricator"),
                descriptor().getDefaultPriority(),
                ThermalSequentialFabricatorAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "thermal_sequential_fabricator_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "cofh.thermalexpansion.gui.container.machine.ContainerCrafter"));
    }
}
