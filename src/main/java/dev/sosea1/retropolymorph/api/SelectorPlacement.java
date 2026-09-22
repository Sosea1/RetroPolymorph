package dev.sosea1.retropolymorph.api;

/**
 * Immutable placement descriptor for the recipe selector button in a GUI container.
 */
public final class SelectorPlacement {

    public enum AnchorMode {
        RESULT_SLOT,
        GUI_TOP_RIGHT,
        ABSOLUTE,
        HIDDEN
    }

    private static final SelectorPlacement HIDDEN_INSTANCE =
            new SelectorPlacement(AnchorMode.HIDDEN, 0, 0, 0, 0, false);

    private final AnchorMode mode;
    private final int anchorX;
    private final int anchorY;
    private final int offsetX;
    private final int offsetY;
    private final boolean visible;

    private SelectorPlacement(
            AnchorMode mode,
            int anchorX,
            int anchorY,
            int offsetX,
            int offsetY,
            boolean visible) {
        this.mode = mode;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.visible = visible;
    }

    public static SelectorPlacement hidden() {
        return HIDDEN_INSTANCE;
    }

    public static SelectorPlacement resultSlot(int anchorX, int anchorY) {
        return resultSlot(anchorX, anchorY, 0, 0);
    }

    public static SelectorPlacement resultSlot(int anchorX, int anchorY, int offsetX, int offsetY) {
        return new SelectorPlacement(AnchorMode.RESULT_SLOT, anchorX, anchorY, offsetX, offsetY, true);
    }

    public static SelectorPlacement guiTopRight(int rightInset, int topInset) {
        return guiTopRight(rightInset, topInset, 0, 0);
    }

    public static SelectorPlacement guiTopRight(int rightInset, int topInset, int offsetX, int offsetY) {
        return new SelectorPlacement(AnchorMode.GUI_TOP_RIGHT, rightInset, topInset, offsetX, offsetY, true);
    }

    public static SelectorPlacement absolute(int x, int y) {
        return absolute(x, y, 0, 0);
    }

    public static SelectorPlacement absolute(int x, int y, int offsetX, int offsetY) {
        return new SelectorPlacement(AnchorMode.ABSOLUTE, x, y, offsetX, offsetY, true);
    }

    public AnchorMode getMode() {
        return this.mode;
    }

    public int getAnchorX() {
        return this.anchorX;
    }

    public int getAnchorY() {
        return this.anchorY;
    }

    public int getOffsetX() {
        return this.offsetX;
    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectorPlacement that = (SelectorPlacement) o;
        return this.anchorX == that.anchorX
                && this.anchorY == that.anchorY
                && this.offsetX == that.offsetX
                && this.offsetY == that.offsetY
                && this.visible == that.visible
                && this.mode == that.mode;
    }

    @Override
    public int hashCode() {
        int result = this.mode.hashCode();
        result = 31 * result + this.anchorX;
        result = 31 * result + this.anchorY;
        result = 31 * result + this.offsetX;
        result = 31 * result + this.offsetY;
        result = 31 * result + (this.visible ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SelectorPlacement{" +
                "mode=" + this.mode +
                ", anchorX=" + this.anchorX +
                ", anchorY=" + this.anchorY +
                ", offsetX=" + this.offsetX +
                ", offsetY=" + this.offsetY +
                ", visible=" + this.visible +
                '}';
    }
}
