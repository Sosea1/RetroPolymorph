package dev.sosea1.retropolymorph.client;

/**
 * Keyboard focus for the recipe selector, kept separate from the server-owned
 * recipe selection. Moving focus never changes the crafted recipe until the
 * user confirms it.
 */
final class SelectorNavigationState {

    private int focusedIndex = -1;

    int getFocusedIndex() {
        return this.focusedIndex;
    }

    void reset() {
        this.focusedIndex = -1;
    }

    void focusSelectedOrFirst(int choiceCount, int selectedIndex) {
        if (choiceCount <= 0) {
            reset();
            return;
        }
        if (isValid(selectedIndex, choiceCount)) {
            this.focusedIndex = selectedIndex;
            return;
        }
        if (!isValid(this.focusedIndex, choiceCount)) {
            this.focusedIndex = 0;
        }
    }

    int cycleFromSelection(int selectedIndex, int delta, int choiceCount) {
        if (choiceCount <= 0 || delta == 0) {
            reset();
            return -1;
        }

        int base = isValid(selectedIndex, choiceCount)
                ? selectedIndex
                : (delta > 0 ? -1 : 0);
        int next = (base + delta) % choiceCount;
        if (next < 0) {
            next += choiceCount;
        }
        this.focusedIndex = next;
        return next;
    }

    boolean moveBy(int delta, int choiceCount) {
        if (choiceCount <= 0) {
            reset();
            return false;
        }
        int previous = normalizedFocus(choiceCount);
        int next = clamp(previous + delta, 0, choiceCount - 1);
        this.focusedIndex = next;
        return next != previous;
    }

    boolean moveToStart(int choiceCount) {
        return setFocusedIndex(0, choiceCount);
    }

    boolean moveToEnd(int choiceCount) {
        return setFocusedIndex(choiceCount - 1, choiceCount);
    }

    boolean setFocusedIndex(int index, int choiceCount) {
        if (!isValid(index, choiceCount)) {
            return false;
        }
        boolean changed = this.focusedIndex != index;
        this.focusedIndex = index;
        return changed;
    }

    void keepInsideVisibleRange(int startInclusive, int endExclusive, int choiceCount) {
        if (choiceCount <= 0 || startInclusive < 0 || endExclusive <= startInclusive) {
            reset();
            return;
        }
        int start = clamp(startInclusive, 0, choiceCount - 1);
        int end = clamp(endExclusive - 1, start, choiceCount - 1);
        int current = normalizedFocus(choiceCount);
        if (current < start) {
            this.focusedIndex = start;
        } else if (current > end) {
            this.focusedIndex = end;
        }
    }

    private int normalizedFocus(int choiceCount) {
        if (!isValid(this.focusedIndex, choiceCount)) {
            this.focusedIndex = 0;
        }
        return this.focusedIndex;
    }

    private static boolean isValid(int index, int choiceCount) {
        return index >= 0 && index < choiceCount;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
