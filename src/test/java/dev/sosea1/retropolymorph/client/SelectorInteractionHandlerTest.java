package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.config.SelectorMode;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SelectorInteractionHandlerTest {

    @Test
    public void testKeyboardNavigationActions() {
        // Expanded with 10 choices, visible 5, indices 0..5
        SelectorAction esc = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_ESCAPE, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.CLOSE, esc.getType());

        SelectorAction left = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_LEFT, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.MOVE_FOCUS, left.getType());
        assertEquals(-1, left.getValue());

        SelectorAction right = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_RIGHT, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.MOVE_FOCUS, right.getType());
        assertEquals(1, right.getValue());

        SelectorAction home = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_HOME, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.FOCUS_ABSOLUTE, home.getType());
        assertEquals(0, home.getValue());

        SelectorAction end = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_END, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.FOCUS_ABSOLUTE, end.getType());
        assertEquals(9, end.getValue());

        SelectorAction pgUp = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_PRIOR, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.MOVE_FOCUS, pgUp.getType());
        assertEquals(-5, pgUp.getValue());

        SelectorAction pgDn = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_NEXT, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.MOVE_FOCUS, pgDn.getType());
        assertEquals(5, pgDn.getValue());

        SelectorAction enter = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_RETURN, true, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.SELECT_FOCUSED, enter.getType());

        // Number keys 1..9
        SelectorAction num1 = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_1, true, true, 10, 5, 2, 7);
        assertEquals(SelectorAction.Type.SELECT_INDEX, num1.getType());
        assertEquals(2, num1.getValue()); // startIndex (2) + 0

        SelectorAction num3 = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_3, true, true, 10, 5, 2, 7);
        assertEquals(SelectorAction.Type.SELECT_INDEX, num3.getType());
        assertEquals(4, num3.getValue()); // startIndex (2) + 2
    }

    @Test
    public void testKeyboardIgnoredWhenClosedOrHidden() {
        SelectorAction hidden = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_RIGHT, true, false, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.NONE, hidden.getType());

        SelectorAction closed = SelectorInteractionHandler.translateKeyboard(
                Keyboard.KEY_RIGHT, false, true, 10, 5, 0, 5);
        assertEquals(SelectorAction.Type.NONE, closed.getType());
    }

    @Test
    public void testMouseActionsOnButton() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(200, 200, 0, 0, 20, 20, 10);

        // Left click button -> toggle
        SelectorAction leftClick = SelectorInteractionHandler.translateMouse(
                10, 10, 0, true, 0,
                true, false, true, true,
                layout, 0, 0, 20, 20, 10);
        assertEquals(SelectorAction.Type.TOGGLE, leftClick.getType());

        // Right click button -> clearToAuto (when enabled)
        SelectorAction rightClick = SelectorInteractionHandler.translateMouse(
                10, 10, 1, true, 0,
                true, false, true, true,
                layout, 0, 0, 20, 20, 10);
        assertEquals(SelectorAction.Type.CLEAR_TO_AUTO, rightClick.getType());

        // Wheel over button -> cycleSelection
        SelectorAction wheelUp = SelectorInteractionHandler.translateMouse(
                10, 10, -1, false, 120,
                true, false, true, true,
                layout, 0, 0, 20, 20, 10);
        assertEquals(SelectorAction.Type.CYCLE_SELECTION, wheelUp.getType());
        assertEquals(-1, wheelUp.getValue());
    }

    @Test
    public void testMouseOutsidePanelCloses() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(200, 200, 0, 0, 20, 20, 10);

        // Click outside panel and outside button -> close
        SelectorAction outside = SelectorInteractionHandler.translateMouse(
                500, 500, 0, true, 0,
                true, true, true, true,
                layout, 0, 0, 20, 20, 10);
        assertEquals(SelectorAction.Type.CLOSE, outside.getType());
    }
}
