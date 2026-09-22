package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.config.SelectorMode;

/**
 * One-row recipe selector layout with two intentionally different UX modes.
 *
 * COMPACT is RetroPolymorph's default: at most five recipe cells are visible and
 * larger conflict sets are navigated with inline arrows or the mouse wheel.
 * CLASSIC mirrors upstream Polymorph: every authoritative option (server-capped
 * at 15) is displayed in one centered strip when it fits. On narrow scaled
 * screens it degrades to a navigable viewport instead of rendering off-screen.
 */
final class SelectorLayout {

    static final int CELL_SIZE = 25;
    static final int COMPACT_VISIBLE = 5;
    static final int NAV_ARROW_WIDTH = 12;

    private final SelectorMode mode;

    private int offset;
    private int visibleCount;
    private int choiceCount;
    private boolean hasNav;

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;

    SelectorLayout(SelectorMode mode) {
        this.mode = mode == null ? SelectorMode.COMPACT : mode;
    }

    SelectorMode getMode() {
        return this.mode;
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

        int fullStripCapacity = maxVisibleCells(guiWidth, false);
        if (this.mode == SelectorMode.CLASSIC && this.choiceCount <= fullStripCapacity) {
            this.hasNav = false;
            this.offset = 0;
            this.visibleCount = Math.max(1, this.choiceCount);
        } else {
            int preferredVisible = this.mode == SelectorMode.CLASSIC
                    ? this.choiceCount
                    : Math.min(COMPACT_VISIBLE, Math.max(1, this.choiceCount));
            int noNavVisible = Math.min(preferredVisible, fullStripCapacity);
            this.hasNav = this.choiceCount > noNavVisible;
            if (this.hasNav) {
                this.visibleCount = Math.min(
                        preferredVisible,
                        maxVisibleCells(guiWidth, true));
            } else {
                this.visibleCount = Math.max(1, this.choiceCount);
                this.offset = 0;
            }
            clampOffset();
        }

        int width = (this.hasNav ? NAV_ARROW_WIDTH * 2 : 0) + this.visibleCount * CELL_SIZE;
        int height = CELL_SIZE;

        // Both modes are centered on the selector button, matching upstream's
        // horizontal selection strip while letting COMPACT reserve arrow space.
        int left = buttonX + (buttonWidth / 2) - (width / 2);
        left = clamp(left, 2, Math.max(2, guiWidth - width - 2));

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

    int getMaxOffset() {
        return Math.max(0, this.choiceCount - this.visibleCount);
    }

    boolean canMoveLeft() {
        return this.hasNav && this.offset > 0;
    }

    boolean canMoveRight() {
        return this.hasNav && this.offset < getMaxOffset();
    }

    void pageLeft() {
        if (!this.hasNav) {
            this.offset = 0;
            return;
        }
        if (this.offset == getMaxOffset() && this.offset % this.visibleCount != 0) {
            this.offset = (this.offset / this.visibleCount) * this.visibleCount;
        } else {
            this.offset = Math.max(0, this.offset - this.visibleCount);
        }
    }

    void pageRight() {
        if (!this.hasNav) {
            this.offset = 0;
            return;
        }
        this.offset = Math.min(getMaxOffset(), this.offset + this.visibleCount);
    }

    void scrollWheel(int direction) {
        if (!this.hasNav) {
            this.offset = 0;
            return;
        }
        this.offset += direction;
        clampOffset();
    }

    void moveOffset(int direction) {
        scrollWheel(direction);
    }

    /** Keep a selected/focused recipe visible whenever the layout uses a viewport. */
    void ensureVisible(int absoluteChoiceIndex) {
        if (!this.hasNav || absoluteChoiceIndex < 0 || absoluteChoiceIndex >= this.choiceCount) {
            return;
        }
        if (absoluteChoiceIndex < this.offset) {
            this.offset = absoluteChoiceIndex;
        } else if (absoluteChoiceIndex >= this.offset + this.visibleCount) {
            this.offset = absoluteChoiceIndex - this.visibleCount + 1;
        }
        clampOffset();
    }

    private void clampOffset() {
        if (!this.hasNav) {
            this.offset = 0;
            return;
        }
        int maxOffset = getMaxOffset();
        if (this.offset < 0) {
            this.offset = 0;
        } else if (this.offset > maxOffset) {
            this.offset = maxOffset;
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

    int getVisibleCount() {
        return this.visibleCount;
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


    private static int maxVisibleCells(int guiWidth, boolean withNavigation) {
        int reserved = 4 + (withNavigation ? NAV_ARROW_WIDTH * 2 : 0);
        int available = Math.max(CELL_SIZE, guiWidth - reserved);
        return Math.max(1, available / CELL_SIZE);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
