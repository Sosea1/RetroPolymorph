package dev.sosea1.retropolymorph.compat.refinedstorage;

import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class RefinedStorageIntegration implements IntegrationRegistrar {

    public static final RefinedStorageIntegration INSTANCE = new RefinedStorageIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "refinedstorage",
            "Refined Storage",
            CompatibilityBootstrap.PRIORITY_NETWORK_GRID);

    private RefinedStorageIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationRefinedStorageEnabled();
    }

    @Override
    public void registerEnabled() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "refinedstorage_crafting_grid"),
                priority,
                RefinedStorageCraftingGridAdapter.INSTANCE);
        registerAdapter(
                new ResourceLocation("retropolymorph", "refinedstorage_pattern_grid"),
                priority,
                RefinedStoragePatternGridAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "refinedstorage_crafting_grid_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "com.raoulvdberge.refinedstorage.container.ContainerGrid"));
    }
}
