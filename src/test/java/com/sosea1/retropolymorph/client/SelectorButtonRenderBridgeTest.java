package com.sosea1.retropolymorph.client;

import gregtech.api.gui.impl.ModularUIGui;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectorButtonRenderBridgeTest {

    @Test
    void legacyGregTechGetsOneButtonDrawAfterItsCustomScreenRender() {
        AtomicInteger draws = new AtomicInteger();
        SelectorButtonRenderBridge.drawIfNeeded(ModularUIGui.class, draws::incrementAndGet);
        assertEquals(1, draws.get());
    }

    @Test
    void legacyGregTechSubclassAlsoGetsTheMissingButtonDraw() {
        AtomicInteger draws = new AtomicInteger();
        SelectorButtonRenderBridge.drawIfNeeded(DerivedGregTechGui.class, draws::incrementAndGet);
        assertEquals(1, draws.get());
    }

    @Test
    void otherScreensKeepTheirNativeButtonDrawWithoutADuplicate() {
        AtomicInteger draws = new AtomicInteger();
        SelectorButtonRenderBridge.drawIfNeeded(Object.class, draws::incrementAndGet);
        assertEquals(0, draws.get());
    }

    @Test
    void modernModularUiUsedByRsbDoesNotGetADuplicateButtonDraw() {
        AtomicInteger draws = new AtomicInteger();
        SelectorButtonRenderBridge.drawIfNeeded(GuiContainerWrapper.class, draws::incrementAndGet);
        assertEquals(0, draws.get());
    }

    private static final class DerivedGregTechGui extends ModularUIGui {
    }
}
