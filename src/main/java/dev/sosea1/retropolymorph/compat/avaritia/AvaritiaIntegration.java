package dev.sosea1.retropolymorph.compat.avaritia;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public enum AvaritiaIntegration implements IntegrationRegistrar {
    INSTANCE;

    private static final String CONTAINER =
            "morph.avaritia.container.ContainerExtremeCrafting";
    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "avaritia",
            "Avaritia",
            CompatibilityBootstrap.PRIORITY_SPECIALIZED_CRAFTER);

    private static final ResourceLocation ADAPTER_ID =
            new ResourceLocation("retropolymorph", "avaritia_extreme_crafting");

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationAvaritiaEnabled();
    }

    @Override
    public void registerEnabled() {
        registerMachineAdapter(
                ADAPTER_ID,
                descriptor().getDefaultPriority(),
                AvaritiaExtremeCraftingAdapter.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "avaritia_extreme_crafting_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(CONTAINER));
    }
}
