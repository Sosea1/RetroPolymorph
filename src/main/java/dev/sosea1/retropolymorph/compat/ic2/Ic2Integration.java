package dev.sosea1.retropolymorph.compat.ic2;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class Ic2Integration implements IntegrationRegistrar {

    public static final Ic2Integration INSTANCE = new Ic2Integration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "ic2",
            "IndustrialCraft 2",
            CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

    private Ic2Integration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationIc2Enabled();
    }

    @Override
    public void registerEnabled() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "ic2_industrial_workbench"),
                priority,
                Ic2IndustrialWorkbenchAdapter.INSTANCE);
        registerAdapter(
                new ResourceLocation("retropolymorph", "ic2_batch_crafter"),
                priority,
                Ic2BatchCrafterAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "ic2_industrial_workbench_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("ic2.core.block.machine.container.ContainerIndustrialWorkbench"));
        registerAdapter(
                new ResourceLocation("retropolymorph", "ic2_batch_crafter_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("ic2.core.block.machine.container.ContainerBatchCrafter"));
    }
}
