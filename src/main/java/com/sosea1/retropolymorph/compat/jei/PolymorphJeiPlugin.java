package com.sosea1.retropolymorph.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;

@JEIPlugin
public final class PolymorphJeiPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        if (!com.sosea1.retropolymorph.config.PolymorphConfig.isIntegrationJeiEnabled()) {
            return;
        }
        registry.addAdvancedGuiHandlers(new PolymorphAdvancedGuiHandler());
    }
}
