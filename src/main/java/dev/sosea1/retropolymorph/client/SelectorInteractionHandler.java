package dev.sosea1.retropolymorph.client;

import org.lwjgl.input.Keyboard;

/**
 * Translates raw keyboard, mouse, and wheel events into high-level {@link SelectorAction}s.
 * Does not render or communicate with the network.
 */
public final class SelectorInteractionHandler {

    private SelectorInteractionHandler() {
    }

    public static SelectorAction translateKeyboard(
            int keyCode,
            boolean expanded,
            boolean buttonVisible,
            int choiceCount,
            int visibleCount,
            int startIndex,
            int endIndex) {
        if (!expanded || !buttonVisible) {
            return SelectorAction.none();
        }
        if (choiceCount == 0) {
            return SelectorAction.close();
        }

        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                return SelectorAction.close();
            case Keyboard.KEY_LEFT:
                return SelectorAction.moveFocus(-1);
            case Keyboard.KEY_RIGHT:
                return SelectorAction.moveFocus(1);
            case Keyboard.KEY_HOME:
                return SelectorAction.focusAbsolute(0);
            case Keyboard.KEY_END:
                return SelectorAction.focusAbsolute(choiceCount - 1);
            case Keyboard.KEY_PRIOR:
                return SelectorAction.moveFocus(-Math.max(1, visibleCount));
            case Keyboard.KEY_NEXT:
                return SelectorAction.moveFocus(Math.max(1, visibleCount));
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                return SelectorAction.selectFocused();
            default:
                if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9) {
                    int visibleIndex = keyCode - Keyboard.KEY_1;
                    int absoluteIndex = startIndex + visibleIndex;
                    if (absoluteIndex < endIndex && absoluteIndex < choiceCount) {
                        return SelectorAction.selectIndex(absoluteIndex);
                    }
                }
                return SelectorAction.none();
        }
    }

    public static SelectorAction translateMouse(
            int mouseX,
            int mouseY,
            int mouseButton,
            boolean pressed,
            int wheel,
            boolean buttonVisible,
            boolean expanded,
            boolean wheelCyclesButton,
            boolean rightClickClears,
            SelectorLayout layout,
            int buttonX,
            int buttonY,
            int buttonWidth,
            int buttonHeight,
            int choiceCount) {
        if (!buttonVisible) {
            return SelectorAction.none();
        }

        if (wheel != 0
                && wheelCyclesButton
                && contains(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight)) {
            return SelectorAction.cycleSelection(wheel < 0 ? 1 : -1);
        }

        if (pressed
                && (mouseButton == 0 || mouseButton == 1)
                && contains(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight)) {
            if (mouseButton == 0) {
                return SelectorAction.toggle();
            } else if (rightClickClears) {
                return SelectorAction.clearToAuto();
            }
            return SelectorAction.none();
        }

        if (!expanded) {
            return SelectorAction.none();
        }

        boolean insidePanel = layout.isInsidePanel(mouseX, mouseY);

        if (wheel != 0) {
            if (insidePanel) {
                return SelectorAction.scrollPanel(wheel < 0 ? 1 : -1);
            }
            return SelectorAction.none();
        }

        if (!pressed) {
            return SelectorAction.none();
        }

        if (!insidePanel) {
            return SelectorAction.close();
        }

        if (mouseButton != 0) {
            return SelectorAction.none();
        }

        if (layout.isLeftArrow(mouseX, mouseY)) {
            if (layout.canMoveLeft()) {
                return SelectorAction.pageLeft();
            }
            return SelectorAction.none();
        } else if (layout.isRightArrow(mouseX, mouseY)) {
            if (layout.canMoveRight()) {
                return SelectorAction.pageRight();
            }
            return SelectorAction.none();
        }

        int choiceIndex = layout.choiceAt(mouseX, mouseY);
        if (choiceIndex >= 0 && choiceIndex < choiceCount) {
            return SelectorAction.selectIndex(choiceIndex);
        }

        return SelectorAction.none();
    }

    public static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
