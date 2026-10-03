package dev.sosea1.retropolymorph.compat.gregtech;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public enum GregTechIntegration implements IntegrationRegistrar {
    INSTANCE;

    private static final ResourceLocation ADAPTER_ID =
            new ResourceLocation("retropolymorph", "gregtech_workbench");

    @Override
    public IntegrationDescriptor descriptor() {
        return new IntegrationDescriptor(
                "gregtech",
                "GregTech CE / CEu",
                CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationGregTechEnabled();
    }

    @Override
    public void registerEnabled() {
        registerAdapter(
                ADAPTER_ID,
                CompatibilityBootstrap.PRIORITY_STANDARD_BENCH,
                GregTechWorkbenchAdapter.INSTANCE);
        dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProviders.register(
                GregTechExternalCraftingSelectionProvider.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        // Generic detection does not recognize either GregTech workbench UI.
    }
}
