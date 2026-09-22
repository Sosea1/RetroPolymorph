package dev.sosea1.retropolymorph.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import zone.rong.mixinbooter.service.ModDiscoverer;

/**
 * Registers optional compatibility mixins only when their target mod is present.
 *
 * <p>Retro Sophisticated Backpacks is intentionally absent: its crafting upgrade
 * uses the common CraftingManager mixin plus an external-selection provider, so
 * it does not need an RSB-specific mixin configuration.</p>
 */
public final class RetroPolymorphMixinConnector implements IMixinConnector {

    private static final String AE2_MOD_ID = "appliedenergistics2";
    private static final String AE2_MIXIN_CONFIG = "mixins.retropolymorph.ae2.json";

    private static final String EXTENDED_CRAFTING_MOD_ID = "extendedcrafting";
    private static final String EXTENDED_CRAFTING_MIXIN_CONFIG =
            "mixins.retropolymorph.extendedcrafting.json";

    private static final String JEI_MOD_ID = "jei";
    private static final String JEI_MIXIN_CONFIG = "mixins.retropolymorph.jei.json";

    private static final String IC2_MOD_ID = "ic2";
    private static final String IC2_MIXIN_CONFIG = "mixins.retropolymorph.ic2.json";

    private static final String CYCLIC_MOD_ID = "cyclicmagic";
    private static final String CYCLIC_MIXIN_CONFIG = "mixins.retropolymorph.cyclic.json";

    private static final String RFTOOLS_CONTROL_MOD_ID = "rftoolscontrol";
    private static final String RFTOOLS_CONTROL_MIXIN_CONFIG =
            "mixins.retropolymorph.rftoolscontrol.json";

    private static final String ENDER_IO_MOD_ID = "enderio";
    private static final String ENDER_IO_MIXIN_CONFIG =
            "mixins.retropolymorph.enderio.json";

    private static final String THAUMCRAFT_MOD_ID = "thaumcraft";
    private static final String THAUMCRAFT_MIXIN_CONFIG =
            "mixins.retropolymorph.thaumcraft.json";

    private static final String REFINED_STORAGE_MOD_ID = "refinedstorage";
    private static final String REFINED_STORAGE_MIXIN_CONFIG =
            "mixins.retropolymorph.refinedstorage.json";

    private static final String MEKANISM_MOD_ID = "mekanism";
    private static final String MEKANISM_MIXIN_CONFIG =
            "mixins.retropolymorph.mekanism.json";

    private static final String THERMAL_EXPANSION_MOD_ID = "thermalexpansion";
    private static final String THERMAL_EXPANSION_MIXIN_CONFIG =
            "mixins.retropolymorph.thermal.json";

    private static final String EXTRA_UTILITIES_2_MOD_ID = "extrautils2";
    private static final String EXTRA_UTILITIES_2_MIXIN_CONFIG =
            "mixins.retropolymorph.extrautils2.json";

    private static final String TCONSTRUCT_MOD_ID = "tconstruct";
    private static final String TCONSTRUCT_MIXIN_CONFIG =
            "mixins.retropolymorph.tconstruct.json";

    @Override
    public void connect() {
        if (ModDiscoverer.isModPresent(AE2_MOD_ID)) {
            Mixins.addConfiguration(AE2_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(EXTENDED_CRAFTING_MOD_ID)) {
            Mixins.addConfiguration(EXTENDED_CRAFTING_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(JEI_MOD_ID)) {
            Mixins.addConfiguration(JEI_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(IC2_MOD_ID)) {
            Mixins.addConfiguration(IC2_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(CYCLIC_MOD_ID)) {
            Mixins.addConfiguration(CYCLIC_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(RFTOOLS_CONTROL_MOD_ID)) {
            Mixins.addConfiguration(RFTOOLS_CONTROL_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(ENDER_IO_MOD_ID)) {
            Mixins.addConfiguration(ENDER_IO_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(THAUMCRAFT_MOD_ID)) {
            Mixins.addConfiguration(THAUMCRAFT_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(REFINED_STORAGE_MOD_ID)) {
            Mixins.addConfiguration(REFINED_STORAGE_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(MEKANISM_MOD_ID)) {
            Mixins.addConfiguration(MEKANISM_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(THERMAL_EXPANSION_MOD_ID)) {
            Mixins.addConfiguration(THERMAL_EXPANSION_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(EXTRA_UTILITIES_2_MOD_ID)) {
            Mixins.addConfiguration(EXTRA_UTILITIES_2_MIXIN_CONFIG);
        }
        if (ModDiscoverer.isModPresent(TCONSTRUCT_MOD_ID)) {
            Mixins.addConfiguration(TCONSTRUCT_MIXIN_CONFIG);
        }
    }
}
