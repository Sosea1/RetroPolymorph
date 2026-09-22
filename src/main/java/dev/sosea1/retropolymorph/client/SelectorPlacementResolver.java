package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.mixin.GuiContainerAccessor;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.lang.reflect.Field;

/**
 * Resolves screen coordinates for the selector button based on {@link SelectorPlacement}
 * and active GUI container bounds (handling Mantle GuiMultiModule offsets and ModularUI corners).
 */
public final class SelectorPlacementResolver {

    public static final class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }
    }

    private static final Field CORNER_X_FIELD;
    private static final Field CORNER_Y_FIELD;

    static {
        Field cx = null;
        Field cy = null;
        try {
            Class<?> gmm = Class.forName("slimeknights.mantle.client.gui.GuiMultiModule");
            cx = gmm.getField("cornerX");
            cy = gmm.getField("cornerY");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }
        CORNER_X_FIELD = cx;
        CORNER_Y_FIELD = cy;
    }

    private SelectorPlacementResolver() {
    }

    /**
     * Resolves the screen (x, y) for the Polymorph button.
     *
     * @param gui the open GUI container
     * @param placement the placement descriptor from context
     * @param configOffsetX user-configured X offset
     * @param configOffsetY user-configured Y offset
     * @param buttonSize width/height of the button in pixels
     * @return clamped screen coordinates (Position)
     */
    public static Position resolveButtonPosition(
            GuiContainer gui,
            SelectorPlacement placement,
            int configOffsetX,
            int configOffsetY,
            int buttonSize) {
        if (!placement.isVisible()) {
            return new Position(0, 0);
        }

        int x;
        int y;
        SelectorPlacement.AnchorMode mode = placement.getMode();
        if (mode == SelectorPlacement.AnchorMode.ABSOLUTE) {
            x = placement.getAnchorX();
            y = placement.getAnchorY();
        } else if (mode == SelectorPlacement.AnchorMode.GUI_TOP_RIGHT) {
            GuiContainerAccessor accessor = (GuiContainerAccessor) gui;
            x = accessor.retropolymorph$getGuiLeft()
                    + accessor.retropolymorph$getXSize()
                    - buttonSize
                    - placement.getAnchorX();
            y = accessor.retropolymorph$getGuiTop()
                    + placement.getAnchorY();
        } else {
            GuiContainerAccessor accessor = (GuiContainerAccessor) gui;
            int guiLeft = accessor.retropolymorph$getGuiLeft();
            int guiTop = accessor.retropolymorph$getGuiTop();

            if (CORNER_X_FIELD != null && CORNER_X_FIELD.getDeclaringClass().isInstance(gui)) {
                try {
                    guiLeft = CORNER_X_FIELD.getInt(gui);
                    guiTop = CORNER_Y_FIELD.getInt(gui);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }

            x = guiLeft
                    + placement.getAnchorX()
                    + (16 - buttonSize) / 2;
            y = guiTop + placement.getAnchorY() - 22;
        }

        x += configOffsetX + placement.getOffsetX();
        y += configOffsetY + placement.getOffsetY();

        int clampedX = clamp(x, 2, Math.max(2, gui.width - buttonSize - 2));
        int clampedY = clamp(y, 2, Math.max(2, gui.height - buttonSize - 2));
        return new Position(clampedX, clampedY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
