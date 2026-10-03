package dev.sosea1.retropolymorph.compat.artisanworktables;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public enum ArtisanWorktablesIntegration implements IntegrationRegistrar {
    INSTANCE;

    private static final String CONTAINER =
            "com.codetaylor.mc.artisanworktables.modules.worktables.gui.AWContainer";
    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "artisanworktables",
            "Artisan Worktables",
            CompatibilityBootstrap.PRIORITY_SPECIALIZED_CRAFTER);

    private static final ResourceLocation ADAPTER_ID =
            new ResourceLocation("retropolymorph", "artisan_worktable");

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationArtisanWorktablesEnabled();
    }

    @Override
    public void registerEnabled() {
        registerMachineAdapter(
                ADAPTER_ID,
                descriptor().getDefaultPriority(),
                ArtisanWorktableAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "artisan_worktable_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(CONTAINER));
    }
}
