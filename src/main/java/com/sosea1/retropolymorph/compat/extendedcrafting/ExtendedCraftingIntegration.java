package com.sosea1.retropolymorph.compat.extendedcrafting;

import com.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import com.sosea1.retropolymorph.compat.IntegrationDescriptor;
import com.sosea1.retropolymorph.compat.IntegrationRegistrar;
import com.sosea1.retropolymorph.compat.PackagePrefixGuardAdapter;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class ExtendedCraftingIntegration implements IntegrationRegistrar {

    public static final ExtendedCraftingIntegration INSTANCE = new ExtendedCraftingIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "extendedcrafting",
            "Extended Crafting",
            CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

    private ExtendedCraftingIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationExtendedCraftingEnabled();
    }

    @Override
    public void registerEnabled() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "extended_crafting_table"),
                priority,
                ExtendedTableAdapter.INSTANCE);
        registerMachineAdapter(
                new ResourceLocation("retropolymorph", "extended_crafting_ender_crafter"),
                priority,
                ExtendedEnderCrafterAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "extended_crafting_table_guard"),
                descriptor().getDefaultPriority(),
                new PackagePrefixGuardAdapter("com.blakebr0.extendedcrafting"));
    }
}
