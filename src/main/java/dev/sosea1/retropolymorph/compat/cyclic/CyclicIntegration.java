package dev.sosea1.retropolymorph.compat.cyclic;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class CyclicIntegration implements IntegrationRegistrar {

    public static final CyclicIntegration INSTANCE = new CyclicIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "cyclicmagic",
            "Cyclic",
            CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

    private CyclicIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationCyclicEnabled();
    }

    @Override
    public void registerEnabled() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "cyclic_workbench"),
                priority,
                CyclicWorkbenchAdapter.INSTANCE);
        registerMachineAdapter(
                new ResourceLocation("retropolymorph", "cyclic_crafter"),
                priority,
                CyclicCrafterAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "cyclic_workbench_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("com.lothrazar.cyclicmagic.block.workbench.ContainerWorkBench"));
        registerAdapter(
                new ResourceLocation("retropolymorph", "cyclic_crafter_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("com.lothrazar.cyclicmagic.block.crafter.ContainerCrafter"));
    }
}
