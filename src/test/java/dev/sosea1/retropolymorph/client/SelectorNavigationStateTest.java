package dev.sosea1.retropolymorph.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SelectorNavigationStateTest {

    @Test
    public void openingFocusesSelectedRecipe() {
        SelectorNavigationState state = new SelectorNavigationState();
        state.focusSelectedOrFirst(8, 5);
        assertEquals(5, state.getFocusedIndex());
    }

    @Test
    public void openingWithoutSelectionFocusesFirstRecipe() {
        SelectorNavigationState state = new SelectorNavigationState();
        state.focusSelectedOrFirst(8, -1);
        assertEquals(0, state.getFocusedIndex());
    }

    @Test
    public void keyboardMovementClampsInsteadOfWrapping() {
        SelectorNavigationState state = new SelectorNavigationState();
        state.focusSelectedOrFirst(4, 0);

        assertFalse(state.moveBy(-1, 4));
        assertEquals(0, state.getFocusedIndex());

        assertTrue(state.moveBy(2, 4));
        assertEquals(2, state.getFocusedIndex());

        assertTrue(state.moveBy(20, 4));
        assertEquals(3, state.getFocusedIndex());
        assertFalse(state.moveBy(1, 4));
    }

    @Test
    public void visibleRangeKeepsFocusOnScreenAfterMousePaging() {
        SelectorNavigationState state = new SelectorNavigationState();
        state.focusSelectedOrFirst(15, 10);

        state.keepInsideVisibleRange(0, 5, 15);
        assertEquals(4, state.getFocusedIndex());

        state.keepInsideVisibleRange(7, 12, 15);
        assertEquals(7, state.getFocusedIndex());
    }

    @Test
    public void emptyChoiceSetClearsFocus() {
        SelectorNavigationState state = new SelectorNavigationState();
        state.focusSelectedOrFirst(3, 2);
        state.focusSelectedOrFirst(0, -1);
        assertEquals(-1, state.getFocusedIndex());
    }

    @Test
    public void cyclesFromSelectedAndWraps() {
        SelectorNavigationState state = new SelectorNavigationState();
        assertEquals(3, state.cycleFromSelection(2, 1, 4));
        assertEquals(0, state.cycleFromSelection(3, 1, 4));
        assertEquals(3, state.cycleFromSelection(0, -1, 4));
    }

    @Test
    public void cyclesFromNativeDefaultWhenNothingSelected() {
        SelectorNavigationState state = new SelectorNavigationState();
        assertEquals(0, state.cycleFromSelection(-1, 1, 4));
        assertEquals(3, state.cycleFromSelection(-1, -1, 4));
    }
}
