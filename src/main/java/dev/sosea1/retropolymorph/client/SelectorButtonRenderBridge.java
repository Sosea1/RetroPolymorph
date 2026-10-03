package dev.sosea1.retropolymorph.client;

/** Extra button rendering for container screens that skip GuiScreen's button list. */
final class SelectorButtonRenderBridge {

    private SelectorButtonRenderBridge() {
    }

    static void drawIfNeeded(Class<?> screenType, Runnable drawButton) {
        for (Class<?> type = screenType; type != null; type = type.getSuperclass()) {
            if ("gregtech.api.gui.impl.ModularUIGui".equals(type.getName())) {
                drawButton.run();
                return;
            }
        }
    }
}
