package com.sosea1.retropolymorph.compat.rftools;

import com.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import com.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import com.sosea1.retropolymorph.compat.IntegrationDescriptor;
import com.sosea1.retropolymorph.compat.IntegrationRegistrar;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class RftoolsIntegration implements IntegrationRegistrar {

    public static final RftoolsIntegration INSTANCE = new RftoolsIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "rftools",
            "RFTools",
            CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

    private RftoolsIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationRftoolsEnabled();
    }

    @Override
    public void registerEnabled() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "rftools_crafter"),
                priority,
                RFToolsCrafterAdapter.INSTANCE);
        registerAdapter(
                new ResourceLocation("retropolymorph", "rftools_workbench"),
                priority,
                RFToolsWorkbenchAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "rftools_crafter_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("mcjty.rftools.blocks.crafter.CrafterContainer"));
        registerAdapter(
                new ResourceLocation("retropolymorph", "rftools_workbench_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("mcjty.rftoolscontrol.blocks.workbench.WorkbenchContainer"));
    }
}
