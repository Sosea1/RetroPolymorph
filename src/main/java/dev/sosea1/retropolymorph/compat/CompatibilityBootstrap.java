package dev.sosea1.retropolymorph.compat;

import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import dev.sosea1.retropolymorph.compat.ae2.Ae2Integration;
import dev.sosea1.retropolymorph.compat.cyclic.CyclicIntegration;
import dev.sosea1.retropolymorph.compat.enderio.EnderIoIntegration;
import dev.sosea1.retropolymorph.compat.extendedcrafting.ExtendedCraftingIntegration;
import dev.sosea1.retropolymorph.compat.extrautils2.ExtraUtils2Integration;
import dev.sosea1.retropolymorph.compat.ic2.Ic2Integration;
import dev.sosea1.retropolymorph.compat.mekanism.MekanismIntegration;
import dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStorageIntegration;
import dev.sosea1.retropolymorph.compat.rftools.RftoolsIntegration;
import dev.sosea1.retropolymorph.compat.rsb.RsbIntegration;
import dev.sosea1.retropolymorph.compat.tconstruct.TinkersIntegration;
import dev.sosea1.retropolymorph.compat.thaumcraft.ThaumcraftIntegration;
import dev.sosea1.retropolymorph.compat.thermal.ThermalIntegration;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Boots built-in mod integrations and guards.
 *
 * <p>Integration-specific registration is owned by each integration package.</p>
 */
public final class CompatibilityBootstrap {

    /** Hard guards for containers that provide their own conflicting native selection UI (e.g. IE). */
    public static final int PRIORITY_NATIVE_RESOLVER_GUARD = 1200;
    public static final int PRIORITY_PACKAGE_GUARD = 1190;

    /** Complex custom UI containers (e.g. ModularUI backpack container). */
    public static final int PRIORITY_MODULAR_UI = 1100;

    /** Virtual machine surfaces (e.g. Extra Utilities 2 crafters). */
    public static final int PRIORITY_VIRTUAL_MACHINE = 1090;

    /** Specialized machine crafters (e.g. EnderIO, Thaumcraft). */
    public static final int PRIORITY_SPECIALIZED_CRAFTER = 1000;

    /** Machine crafters with custom recipe slots (e.g. Mekanism). */
    public static final int PRIORITY_MEKANISM = 950;

    /** Fluid-aware machine crafters (e.g. Thermal Expansion). */
    public static final int PRIORITY_THERMAL = 940;

    /** Network grid containers (e.g. Refined Storage). */
    public static final int PRIORITY_NETWORK_GRID = 900;

    /** Standard craft bench integrations (AE2, IC2, Cyclic, Extended Crafting, RFTools, Tinkers, Forestry). */
    public static final int PRIORITY_STANDARD_BENCH = 800;

    private static final List<IntegrationRegistrar> REGISTRARS = Collections.unmodifiableList(
            Arrays.<IntegrationRegistrar>asList(
                    RsbIntegration.INSTANCE,
                    ExtraUtils2Integration.INSTANCE,
                    EnderIoIntegration.INSTANCE,
                    ThaumcraftIntegration.INSTANCE,
                    Ae2Integration.INSTANCE,
                    ExtendedCraftingIntegration.INSTANCE,
                    Ic2Integration.INSTANCE,
                    CyclicIntegration.INSTANCE,
                    RftoolsIntegration.INSTANCE,
                    TinkersIntegration.INSTANCE,
                    dev.sosea1.retropolymorph.compat.forestry.ForestryGuardIntegration.INSTANCE,
                    MekanismIntegration.INSTANCE,
                    ThermalIntegration.INSTANCE,
                    RefinedStorageIntegration.INSTANCE
            ));

    private static boolean initialized;

    private CompatibilityBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        // Global native-selector guards
        IntegrationHealthRegistry.register(
                new IntegrationDescriptor("immersiveengineering", "Immersive Engineering", PRIORITY_NATIVE_RESOLVER_GUARD),
                true);
        IntegrationHealthRegistry.associateAdapter(id("immersive_engineering_mod_workbench_native_selector"), "immersiveengineering");
        RetroPolymorphAPI.registerAdapter(
                id("immersive_engineering_mod_workbench_native_selector"),
                PRIORITY_NATIVE_RESOLVER_GUARD,
                new CustomRecipeEngineGuardAdapter(
                        "blusunrize.immersiveengineering.common.gui.ContainerModWorkbench"));

        // Engineer's Decor 1.12.2 has its own conflict-cycle button only on the
        // treated-wood crafting table. Guard that exact container instead of
        // blocking every GUI in the mod package.
        IntegrationHealthRegistry.register(
                new IntegrationDescriptor("engineersdecor", "Engineer's Decor", PRIORITY_PACKAGE_GUARD),
                true);
        IntegrationHealthRegistry.associateAdapter(id("engineers_decor_native_selector"), "engineersdecor");
        RetroPolymorphAPI.registerAdapter(
                id("engineers_decor_native_selector"),
                PRIORITY_PACKAGE_GUARD,
                new CustomRecipeEngineGuardAdapter(
                        "wile.engineersdecor.blocks.BlockDecorCraftingTable$BContainer"));

        // Modular integration registrations
        for (IntegrationRegistrar registrar : REGISTRARS) {
            IntegrationHealthRegistry.register(registrar.descriptor(), registrar.isEnabled());

            if (registrar.isEnabled()) {
                registrar.registerEnabled();
            } else {
                registrar.registerDisabledGuards();
            }
        }

        dev.sosea1.retropolymorph.api.RecipeSelectionAdapters.setProbeListener(
                IntegrationHealthRegistry.getProbeListener());
        initialized = true;
    }

    public static List<IntegrationRegistrar> getRegistrars() {
        return REGISTRARS;
    }

    static synchronized void resetForTests() {
        initialized = false;
        IntegrationHealthRegistry.resetForTests();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("retropolymorph", path);
    }
}
