package com.sosea1.retropolymorph.compat.enderio;

import com.sosea1.retropolymorph.api.RetroPolymorphAPI;
import com.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import com.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import com.sosea1.retropolymorph.compat.IntegrationDescriptor;
import com.sosea1.retropolymorph.compat.IntegrationRegistrar;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class EnderIoIntegration implements IntegrationRegistrar {

    public static final EnderIoIntegration INSTANCE = new EnderIoIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "enderio",
            "Ender IO",
            CompatibilityBootstrap.PRIORITY_SPECIALIZED_CRAFTER);

    private EnderIoIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationEnderIoEnabled();
    }

    @Override
    public void registerEnabled() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "enderio_crafter"),
                descriptor().getDefaultPriority(),
                EnderIoCrafterAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "enderio_crafter_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "crazypants.enderio.machines.machine.crafter.ContainerCrafter"));
    }
}
