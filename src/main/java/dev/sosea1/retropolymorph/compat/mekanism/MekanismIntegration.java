package dev.sosea1.retropolymorph.compat.mekanism;

import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class MekanismIntegration implements IntegrationRegistrar {

    public static final MekanismIntegration INSTANCE = new MekanismIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "mekanism",
            "Mekanism",
            CompatibilityBootstrap.PRIORITY_MEKANISM);

    private MekanismIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationMekanismEnabled();
    }

    @Override
    public void registerEnabled() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "mekanism_formulaic_assemblicator"),
                descriptor().getDefaultPriority(),
                MekanismFormulaicAssemblicatorAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "mekanism_formulaic_assemblicator_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "mekanism.common.inventory.container.ContainerFormulaicAssemblicator"));
    }
}
