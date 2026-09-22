package dev.sosea1.retropolymorph.compat.forestry;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import net.minecraft.util.ResourceLocation;

/**
 * Native guard for Forestry's Worktable.
 * Prevents generic crafting detectors from interfering with Forestry's native recipe selection.
 */
public final class ForestryGuardIntegration implements IntegrationRegistrar {

    public static final ForestryGuardIntegration INSTANCE = new ForestryGuardIntegration();
    private static final IntegrationDescriptor DESCRIPTOR =
            new IntegrationDescriptor("forestry", "Forestry", CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

    private ForestryGuardIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void registerEnabled() {
        ResourceLocation adapterId = new ResourceLocation("retropolymorph", "forestry_worktable_guard");
        registerAdapter(
                adapterId,
                CompatibilityBootstrap.PRIORITY_STANDARD_BENCH,
                new CustomRecipeEngineGuardAdapter("forestry.worktable.gui.ContainerWorktable"));
    }

    @Override
    public void registerDisabledGuards() {
    }
}
