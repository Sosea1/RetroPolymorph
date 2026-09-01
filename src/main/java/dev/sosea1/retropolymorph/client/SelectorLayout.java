package dev.sosea1.retropolymorph.client;

/**
 * Clean, compact 1-row horizontal selector layout.
 *
 * Displays up to 5 recipe cells at a time. If there are more than 5 choices,
 * compact left (<) and right (>) navigation arrows are rendered inline on the sides,
 * with smooth scroll/paging support.
 */
final class SelectorLayout {

    static final int CELL_SIZE = 25;
    static final int MAX_VISIBLE = 5;
    static final int NAV_ARROW_WIDTH = 12;

    private int offset;
    private int visibleCount;
    private int choiceCount;
    private boolean hasNav;

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;

    SelectorLayout(int configuredColumns, int configuredRows) {
    }

    void resetPage() {
        this.offset = 0;
    }

    void update(
            int guiWidth,
            int guiHeight,
            int buttonX,
            int buttonY,
            int buttonWidth,
            int buttonHeight,
            int choices) {
        this.choiceCount = Math.max(0, choices);
        this.hasNav = this.choiceCount > MAX_VISIBLE;
        this.visibleCount = this.hasNav ? MAX_VISIBLE : Math.max(1, this.choiceCount);

        clampOffset();

        int width = (this.hasNav ? NAV_ARROW_WIDTH * 2 : 0) + this.visibleCount * CELL_SIZE;
        int height = CELL_SIZE;

        // Center on the button
        int left = buttonX + (buttonWidth / 2) - (width / 2);
        left = clamp(left, 2, Math.max(2, guiWidth - width - 2));

        // Position above the button (-28px default offset); flip below if overflowing top
        int top = buttonY - height - 4;
        if (top < 2) {
            top = buttonY + buttonHeight + 4;
            if (top + height > guiHeight - 2) {
                top = guiHeight - height - 2;
            }
        }
        top = clamp(top, 2, Math.max(2, guiHeight - height - 2));

        this.panelLeft = left;
        this.panelTop = top;
        this.panelRight = left + width;
        this.panelBottom = top + height;
    }

    void moveOffset(int direction) {
        if (!this.hasNav) {
            this.offset = 0;
            return;
        }

        this.offset += direction;
        clampOffset();
    }

    private void clampOffset() {
        if (!this.hasNav) {
            this.offset = 0;
            return;
        }
        int maxOffset = Math.max(0, this.choiceCount - MAX_VISIBLE);
        if (this.offset < 0) {
            this.offset = maxOffset;
        } else if (this.offset > maxOffset) {
            this.offset = 0;
        }
    }

    boolean isInsidePanel(int mouseX, int mouseY) {
        return mouseX >= this.panelLeft
                && mouseX < this.panelRight
                && mouseY >= this.panelTop
                && mouseY < this.panelBottom;
    }

    boolean isLeftArrow(int mouseX, int mouseY) {
        return this.hasNav
                && mouseY >= this.panelTop
                && mouseY < this.panelBottom
                && mouseX >= this.panelLeft
                && mouseX < this.panelLeft + NAV_ARROW_WIDTH;
    }

    boolean isRightArrow(int mouseX, int mouseY) {
        return this.hasNav
                && mouseY >= this.panelTop
                && mouseY < this.panelBottom
                && mouseX >= this.panelRight - NAV_ARROW_WIDTH
                && mouseX < this.panelRight;
    }

    int choiceAt(int mouseX, int mouseY) {
        if (mouseY < this.panelTop || mouseY >= this.panelBottom) {
            return -1;
        }

        int cellStartX = this.panelLeft + (this.hasNav ? NAV_ARROW_WIDTH : 0);
        int contentX = mouseX - cellStartX;
        if (contentX < 0) {
            return -1;
        }

        int col = contentX / CELL_SIZE;
        if (col < 0 || col >= this.visibleCount) {
            return -1;
        }

        int index = this.offset + col;
        return index < this.choiceCount ? index : -1;
    }

    int getCellX(int absoluteChoiceIndex) {
        int local = absoluteChoiceIndex - this.offset;
        return this.panelLeft + (this.hasNav ? NAV_ARROW_WIDTH : 0) + local * CELL_SIZE;
    }

    int getCellY(int absoluteChoiceIndex) {
        return this.panelTop;
    }

    int getStartIndex() {
        return this.offset;
    }

    int getEndIndex() {
        return Math.min(this.offset + this.visibleCount, this.choiceCount);
    }

    boolean hasNavigation() {
        return this.hasNav;
    }

    int getPanelLeft() {
        return this.panelLeft;
    }

    int getPanelTop() {
        return this.panelTop;
    }

    int getPanelRight() {
        return this.panelRight;
    }

    int getPanelBottom() {
        return this.panelBottom;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
