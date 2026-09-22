package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSelectionTrackerTest {

    @AfterEach
    void resetTracker() {
        ClientSelectionTracker.reset();
    }

    @Test
    public void authoritativeSnapshotIsOnlyTrueAfterMatchingServerReply() {
        int token = ClientSelectionTracker.begin(7);
        assertFalse(ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(7));

        ClientSelectionTracker.apply(
                7,
                token,
                0,
                true,
                null,
                Collections.<RecipeOption>emptyList());
        assertTrue(ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(7));
    }

    @Test
    public void changingInputRevisionMakesPreviousSnapshotStale() {
        int token = ClientSelectionTracker.begin(9);
        ClientSelectionTracker.apply(
                9,
                token,
                0,
                true,
                null,
                Collections.<RecipeOption>emptyList());
        assertTrue(ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(9));

        ClientSelectionTracker.expectInputRevision(9, token, 1);
        assertFalse(ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(9));
    }
    @Test
    public void provisionalWindowIdCanRebindWithinSameGuiSession() {
        int token = ClientSelectionTracker.begin(0);

        // A reply for the server-assigned id is stale until the live container id is rebound.
        ClientSelectionTracker.expectInputRevision(1, token, 6);
        ClientSelectionTracker.apply(
                1,
                token,
                6,
                true,
                null,
                Collections.<RecipeOption>emptyList());
        assertFalse(ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(1));

        assertTrue(ClientSelectionTracker.rebindWindowId(1, token));
        ClientSelectionTracker.expectInputRevision(1, token, 6);
        ClientSelectionTracker.apply(
                1,
                token,
                6,
                true,
                null,
                Collections.<RecipeOption>emptyList());

        assertTrue(ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(1));
        assertFalse(ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(0));
    }

    @Test
    public void staleSessionCannotRebindNewGuiLifetime() {
        int oldToken = ClientSelectionTracker.begin(0);
        int liveToken = ClientSelectionTracker.begin(0);

        assertFalse(ClientSelectionTracker.rebindWindowId(1, oldToken));
        assertTrue(ClientSelectionTracker.rebindWindowId(1, liveToken));
    }

}
