package dev.sosea1.retropolymorph.compat.rsb;

import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProviders;
import net.minecraft.util.ResourceLocation;

/** Registers the focused RSB crafting adapter and its CraftingManager selection provider. */
public final class RsbIntegration implements IntegrationRegistrar {

    public static final RsbIntegration INSTANCE = new RsbIntegration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "retrosophisticatedbackpacks",
            "Retro Sophisticated Backpacks",
            CompatibilityBootstrap.PRIORITY_MODULAR_UI);

    private RsbIntegration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationRetroSophisticatedBackpacksEnabled();
    }

    @Override
    public void registerEnabled() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "retro_sophisticated_backpacks_crafting_upgrade"),
                descriptor().getDefaultPriority(),
                RetroSophisticatedBackpackAdapter.INSTANCE);
        ExternalCraftingSelectionProviders.register(RsbExternalCraftingSelectionProvider.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        registerAdapter(
                new ResourceLocation("retropolymorph", "retro_sophisticated_backpacks_guard"),
                descriptor().getDefaultPriority(),
                new CustomRecipeEngineGuardAdapter(
                        "com.cleanroommc.retrosophisticatedbackpacks.common.gui.BackpackContainer"));
    }
}
