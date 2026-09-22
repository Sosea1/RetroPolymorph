package dev.sosea1.retropolymorph.compat.tconstruct;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class TinkersIntegration implements IntegrationRegistrar {

    public static final TinkersIntegration INSTANCE = new TinkersIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "tconstruct",
            "Tinkers' Construct",
            CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

    private TinkersIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationTinkersEnabled();
    }

    @Override
    public void registerEnabled() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "tconstruct_crafting_station"),
                descriptor().getDefaultPriority(),
                TinkersCraftingStationAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "tconstruct_crafting_station_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "slimeknights.tconstruct.tools.common.inventory.ContainerCraftingStation"));
    }
}
