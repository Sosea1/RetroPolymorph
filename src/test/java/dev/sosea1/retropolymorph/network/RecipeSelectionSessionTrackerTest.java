package dev.sosea1.retropolymorph.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RecipeSelectionSessionTrackerTest {

    @Test
    public void observesLatestSessionForWindow() {
        RecipeSelectionSessionTracker tracker = new RecipeSelectionSessionTracker();
        UUID player = UUID.randomUUID();

        tracker.observe(player, 7, 41, 3);
        RecipeSelectionSessionTracker.Session session = tracker.get(player, 7);

        assertEquals(7, session.windowId);
        assertEquals(41, session.sessionToken);
        assertEquals(3, session.inputRevision);
        assertNull(tracker.get(player, 8));
    }

    @Test
    public void newerObservationReplacesOldSession() {
        RecipeSelectionSessionTracker tracker = new RecipeSelectionSessionTracker();
        UUID player = UUID.randomUUID();

        tracker.observe(player, 7, 41, 3);
        tracker.observe(player, 8, 42, 0);

        assertNull(tracker.get(player, 7));
        RecipeSelectionSessionTracker.Session session = tracker.get(player, 8);
        assertEquals(42, session.sessionToken);
    }

    @Test
    public void clearWindowDoesNotRemoveDifferentWindow() {
        RecipeSelectionSessionTracker tracker = new RecipeSelectionSessionTracker();
        UUID player = UUID.randomUUID();
        tracker.observe(player, 8, 42, 2);

        RecipeSelectionSessionTracker.Session expected = tracker.get(player, 8);
        tracker.clearWindow(player, 7);

        assertSame(expected, tracker.get(player, 8));
        tracker.clearWindow(player, 8);
        assertNull(tracker.get(player, 8));
    }

    @Test
    public void ignoresInvalidSessionTokens() {
        RecipeSelectionSessionTracker tracker = new RecipeSelectionSessionTracker();
        UUID player = UUID.randomUUID();

        tracker.observe(player, 7, 0, 1);
        tracker.observe(player, -1, 10, 1);

        assertNull(tracker.get(player, 7));
    }
}
