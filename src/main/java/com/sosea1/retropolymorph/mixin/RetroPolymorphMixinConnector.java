package com.sosea1.retropolymorph.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import zone.rong.mixinbooter.service.ModDiscoverer;

/** Registers optional compatibility mixins only when their target mod is present. */
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

    private static final String AVARITIA_MOD_ID = "avaritia";
    private static final String AVARITIA_MIXIN_CONFIG =
            "mixins.retropolymorph.avaritia.json";

    private static final String ARTISAN_WORKTABLES_MOD_ID = "artisanworktables";
    private static final String ARTISAN_WORKTABLES_MIXIN_CONFIG =
            "mixins.retropolymorph.artisanworktables.json";

    @Override
    public void connect() {
        addIfPresent(AE2_MOD_ID, AE2_MIXIN_CONFIG);
        addIfPresent(EXTENDED_CRAFTING_MOD_ID, EXTENDED_CRAFTING_MIXIN_CONFIG);
        addIfPresent(JEI_MOD_ID, JEI_MIXIN_CONFIG);
        addIfPresent(IC2_MOD_ID, IC2_MIXIN_CONFIG);
        addIfPresent(CYCLIC_MOD_ID, CYCLIC_MIXIN_CONFIG);
        addIfPresent(RFTOOLS_CONTROL_MOD_ID, RFTOOLS_CONTROL_MIXIN_CONFIG);
        addIfPresent(ENDER_IO_MOD_ID, ENDER_IO_MIXIN_CONFIG);
        addIfPresent(THAUMCRAFT_MOD_ID, THAUMCRAFT_MIXIN_CONFIG);
        addIfPresent(REFINED_STORAGE_MOD_ID, REFINED_STORAGE_MIXIN_CONFIG);
        addIfPresent(MEKANISM_MOD_ID, MEKANISM_MIXIN_CONFIG);
        addIfPresent(THERMAL_EXPANSION_MOD_ID, THERMAL_EXPANSION_MIXIN_CONFIG);
        addIfPresent(EXTRA_UTILITIES_2_MOD_ID, EXTRA_UTILITIES_2_MIXIN_CONFIG);
        addIfPresent(TCONSTRUCT_MOD_ID, TCONSTRUCT_MIXIN_CONFIG);
        addIfPresent(AVARITIA_MOD_ID, AVARITIA_MIXIN_CONFIG);
        addIfPresent(ARTISAN_WORKTABLES_MOD_ID, ARTISAN_WORKTABLES_MIXIN_CONFIG);
        addIfPresent("gregtech", "mixins.retropolymorph.gregtech.json");
    }

    private static void addIfPresent(String modId, String config) {
        if (ModDiscoverer.isModPresent(modId)) {
            Mixins.addConfiguration(config);
        }
    }
}
