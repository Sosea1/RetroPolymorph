package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.compat.CompatibilityBootstrap;
import dev.sosea1.retropolymorph.compat.CustomRecipeEngineGuardAdapter;
import dev.sosea1.retropolymorph.compat.IntegrationDescriptor;
import dev.sosea1.retropolymorph.compat.IntegrationRegistrar;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.util.ResourceLocation;

public final class Ae2Integration implements IntegrationRegistrar {

    public static final Ae2Integration INSTANCE = new Ae2Integration();

    private static final IntegrationDescriptor DESCRIPTOR = new IntegrationDescriptor(
            "appliedenergistics2",
            "Applied Energistics 2",
            CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

    private Ae2Integration() {
    }

    @Override
    public IntegrationDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isEnabled() {
        return PolymorphConfig.isIntegrationAe2Enabled();
    }

    @Override
    public void registerEnabled() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "ae2_pattern_terminal"),
                priority,
                Ae2PatternTermAdapter.INSTANCE);
        registerAdapter(
                new ResourceLocation("retropolymorph", "ae2_crafting_terminal"),
                priority,
                Ae2CraftingTermAdapter.INSTANCE);
        dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProviders.register(
                Ae2ExternalCraftingSelectionProvider.INSTANCE);
    }

    @Override
    public void registerDisabledGuards() {
        int priority = descriptor().getDefaultPriority();
        registerAdapter(
                new ResourceLocation("retropolymorph", "ae2_pattern_terminal_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("appeng.container.implementations.ContainerPatternTerm"));
        registerAdapter(
                new ResourceLocation("retropolymorph", "ae2_crafting_terminal_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("appeng.container.implementations.ContainerCraftingTerm"));
        registerAdapter(
                new ResourceLocation("retropolymorph", "ae2_wireless_terminal_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("appeng.container.implementations.ContainerWirelessCraftingTerminal"));
        registerAdapter(
                new ResourceLocation("retropolymorph", "ae2_wct_guard"),
                priority,
                new CustomRecipeEngineGuardAdapter("p455w0rd.wct.container.ContainerWCT"));
    }
}
