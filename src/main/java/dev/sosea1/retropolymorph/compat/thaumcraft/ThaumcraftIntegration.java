package dev.sosea1.retropolymorph.compat.thaumcraft;

import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class ThaumcraftIntegration implements IntegrationRegistrar {

    public static final ThaumcraftIntegration INSTANCE = new ThaumcraftIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "thaumcraft",
            "Thaumcraft",
            CompatibilityBootstrap.PRIORITY_SPECIALIZED_CRAFTER);

    private ThaumcraftIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationThaumcraftEnabled();
    }

    @Override
    public void registerEnabled() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "thaumcraft_arcane_workbench"),
                descriptor().getDefaultPriority(),
                ThaumcraftArcaneWorkbenchAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "thaumcraft_arcane_workbench_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "thaumcraft.common.container.ContainerArcaneWorkbench"));
    }
}
