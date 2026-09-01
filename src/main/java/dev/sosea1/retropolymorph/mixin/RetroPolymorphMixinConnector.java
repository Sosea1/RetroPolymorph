package dev.sosea1.retropolymorph.mixin;

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
    }
}
