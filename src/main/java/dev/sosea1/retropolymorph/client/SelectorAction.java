package dev.sosea1.retropolymorph.client;

/**
 * High-level UI action resulting from user interaction (keyboard, mouse, wheel).
 */
public final class SelectorAction {

    public enum Type {
        NONE,
        TOGGLE,
        CLOSE,
        CLEAR_TO_AUTO,
        CYCLE_SELECTION,
        MOVE_FOCUS,
        FOCUS_ABSOLUTE,
        SELECT_FOCUSED,
        SELECT_INDEX,
        SCROLL_PANEL,
        PAGE_LEFT,
        PAGE_RIGHT
    }

    private final Type type;
    private final int value;

    public SelectorAction(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return this.type;
    }

    public int getValue() {
        return this.value;
    }

    public static SelectorAction none() {
        return new SelectorAction(Type.NONE, 0);
    }

    public static SelectorAction toggle() {
        return new SelectorAction(Type.TOGGLE, 0);
    }

    public static SelectorAction close() {
        return new SelectorAction(Type.CLOSE, 0);
    }

    public static SelectorAction clearToAuto() {
        return new SelectorAction(Type.CLEAR_TO_AUTO, 0);
    }

    public static SelectorAction cycleSelection(int delta) {
        return new SelectorAction(Type.CYCLE_SELECTION, delta);
    }

    public static SelectorAction moveFocus(int offset) {
        return new SelectorAction(Type.MOVE_FOCUS, offset);
    }

    public static SelectorAction focusAbsolute(int index) {
        return new SelectorAction(Type.FOCUS_ABSOLUTE, index);
    }

    public static SelectorAction selectFocused() {
        return new SelectorAction(Type.SELECT_FOCUSED, 0);
    }

    public static SelectorAction selectIndex(int index) {
        return new SelectorAction(Type.SELECT_INDEX, index);
    }

    public static SelectorAction scrollPanel(int delta) {
        return new SelectorAction(Type.SCROLL_PANEL, delta);
    }

    public static SelectorAction pageLeft() {
        return new SelectorAction(Type.PAGE_LEFT, 0);
    }

    public static SelectorAction pageRight() {
        return new SelectorAction(Type.PAGE_RIGHT, 0);
    }

    @Override
    public String toString() {
        return "SelectorAction{" + "type=" + this.type + ", value=" + this.value + '}';
    }
}
