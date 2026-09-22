package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.config.SelectorMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SelectorLayoutTest {

    @Test
    public void compactKeepsFiveChoiceWindowForFifteenOptions() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(640, 360, 300, 220, 16, 16, 15);

        assertTrue(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertEquals(5, layout.getEndIndex());
    }

    @Test
    public void compactDoesNotShowArrowsForFiveOrFewerChoices() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(400, 240, 190, 140, 16, 16, 5);

        assertFalse(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertEquals(5, layout.getEndIndex());
    }

    @Test
    public void compactArrowsSlideThroughAuthoritativeOptionsWithoutDroppingThem() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(640, 360, 300, 220, 16, 16, 15);

        layout.moveOffset(1);
        assertEquals(1, layout.getStartIndex());
        assertEquals(6, layout.getEndIndex());

        layout.moveOffset(-1);
        assertEquals(0, layout.getStartIndex());
        assertEquals(5, layout.getEndIndex());
    }

    @Test
    public void compactOpeningCanRevealAnOffPageSelectedRecipe() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(640, 360, 300, 220, 16, 16, 15);

        layout.ensureVisible(12);
        assertTrue(layout.getStartIndex() <= 12);
        assertTrue(layout.getEndIndex() > 12);
        assertEquals(8, layout.getStartIndex());
        assertEquals(13, layout.getEndIndex());
    }

    @Test
    public void classicShowsWholeAuthoritativeSetWithoutNavigation() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.CLASSIC);
        layout.update(640, 360, 300, 220, 16, 16, 15);

        assertFalse(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertEquals(15, layout.getEndIndex());
    }

    @Test
    public void classicIgnoresPagingOperations() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.CLASSIC);
        layout.update(640, 360, 300, 220, 16, 16, 9);

        layout.moveOffset(1);
        layout.ensureVisible(8);
        assertEquals(0, layout.getStartIndex());
        assertEquals(9, layout.getEndIndex());
    }

    @Test
    public void zeroOrOneOptionYieldsEmptyWindow() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(640, 360, 300, 220, 16, 16, 0);
        assertFalse(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertEquals(0, layout.getEndIndex());

        layout.update(640, 360, 300, 220, 16, 16, 1);
        assertFalse(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertEquals(1, layout.getEndIndex());
    }

    @Test
    public void twoOptionsDoesNotShowNavigation() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(640, 360, 300, 220, 16, 16, 2);
        assertFalse(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertEquals(2, layout.getEndIndex());
    }

    @Test
    public void sixOptionsActivatesNavigationInCompactMode() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(640, 360, 300, 220, 16, 16, 6);
        assertTrue(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertEquals(5, layout.getEndIndex());
        assertFalse(layout.canMoveLeft());
        assertTrue(layout.canMoveRight());

        layout.moveOffset(1);
        assertEquals(1, layout.getStartIndex());
        assertEquals(6, layout.getEndIndex());
        assertTrue(layout.canMoveLeft());
        assertFalse(layout.canMoveRight());

        // Strictly clamps at max offset, does not wrap around
        layout.moveOffset(1);
        assertEquals(1, layout.getStartIndex());
        assertEquals(6, layout.getEndIndex());

        // Strictly clamps at 0, does not wrap around
        layout.moveOffset(-2);
        assertEquals(0, layout.getStartIndex());
        assertEquals(5, layout.getEndIndex());
        assertFalse(layout.canMoveLeft());
        assertTrue(layout.canMoveRight());
    }

    @Test
    public void pageNavigationInCompactMode() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(640, 360, 300, 220, 16, 16, 12);
        assertTrue(layout.hasNavigation());
        assertEquals(0, layout.getStartIndex());
        assertFalse(layout.canMoveLeft());
        assertTrue(layout.canMoveRight());

        layout.pageRight();
        assertEquals(5, layout.getStartIndex());
        assertTrue(layout.canMoveLeft());
        assertTrue(layout.canMoveRight());

        layout.pageRight();
        assertEquals(7, layout.getStartIndex()); // clamped to maxOffset = 12 - 5 = 7
        assertTrue(layout.canMoveLeft());
        assertFalse(layout.canMoveRight());

        layout.pageLeft();
        assertEquals(5, layout.getStartIndex());
        assertTrue(layout.canMoveLeft());
        assertTrue(layout.canMoveRight());

        layout.pageLeft();
        assertEquals(0, layout.getStartIndex());
        assertFalse(layout.canMoveLeft());
        assertTrue(layout.canMoveRight());
    }
    @Test
    public void classicFallsBackToViewportWhenFullStripCannotFitScreen() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.CLASSIC);
        layout.update(160, 240, 80, 140, 16, 16, 15);

        assertTrue(layout.hasNavigation());
        assertTrue(layout.getEndIndex() - layout.getStartIndex() < 15);
        assertTrue(layout.getPanelLeft() >= 2);
        assertTrue(layout.getPanelRight() <= 158);

        layout.ensureVisible(14);
        assertTrue(layout.getStartIndex() <= 14);
        assertTrue(layout.getEndIndex() > 14);
    }

    @Test
    public void compactShrinksViewportOnVeryNarrowScreens() {
        SelectorLayout layout = new SelectorLayout(SelectorMode.COMPACT);
        layout.update(100, 180, 45, 100, 16, 16, 8);

        assertTrue(layout.hasNavigation());
        assertTrue(layout.getEndIndex() - layout.getStartIndex() < SelectorLayout.COMPACT_VISIBLE);
        assertTrue(layout.getPanelLeft() >= 2);
        assertTrue(layout.getPanelRight() <= 98);
    }

}
