package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.core.SharedSelectionViewerRegistry.ViewerEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedSelectionViewerRegistryTest {

    @BeforeEach
    @AfterEach
    void setUp() {
        SharedSelectionViewerRegistry.clear();
    }

    @Test
    void testRegisterAndGetViewers() {
        Object owner = new Object();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        SharedSelectionViewerRegistry.register(owner, player1, 1);
        SharedSelectionViewerRegistry.register(owner, player2, 2);

        List<ViewerEntry> viewers = SharedSelectionViewerRegistry.getViewers(owner);
        assertEquals(2, viewers.size());
        assertTrue(viewers.contains(new ViewerEntry(player1, 1)));
        assertTrue(viewers.contains(new ViewerEntry(player2, 2)));
    }

    @Test
    void testDistinctOwnersDoNotCross() {
        Object ownerA = new Object();
        Object ownerB = new Object();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        SharedSelectionViewerRegistry.register(ownerA, player1, 1);
        SharedSelectionViewerRegistry.register(ownerB, player2, 2);

        List<ViewerEntry> viewersA = SharedSelectionViewerRegistry.getViewers(ownerA);
        List<ViewerEntry> viewersB = SharedSelectionViewerRegistry.getViewers(ownerB);

        assertEquals(1, viewersA.size());
        assertEquals(player1, viewersA.get(0).getPlayerId());

        assertEquals(1, viewersB.size());
        assertEquals(player2, viewersB.get(0).getPlayerId());
    }

    @Test
    void testUnregisterWindow() {
        Object owner = new Object();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        SharedSelectionViewerRegistry.register(owner, player1, 1);
        SharedSelectionViewerRegistry.register(owner, player2, 2);

        SharedSelectionViewerRegistry.unregister(player1, 1);

        List<ViewerEntry> viewers = SharedSelectionViewerRegistry.getViewers(owner);
        assertEquals(1, viewers.size());
        assertEquals(player2, viewers.get(0).getPlayerId());

        SharedSelectionViewerRegistry.unregister(player2, 2);
        assertTrue(SharedSelectionViewerRegistry.getViewers(owner).isEmpty());
    }

    @Test
    void testUnregisterPlayerRemovesAllActiveWindows() {
        Object ownerA = new Object();
        Object ownerB = new Object();
        UUID player = UUID.randomUUID();

        SharedSelectionViewerRegistry.register(ownerA, player, 1);
        SharedSelectionViewerRegistry.register(ownerB, player, 2);

        SharedSelectionViewerRegistry.unregisterPlayer(player);

        assertTrue(SharedSelectionViewerRegistry.getViewers(ownerA).isEmpty());
        assertTrue(SharedSelectionViewerRegistry.getViewers(ownerB).isEmpty());
    }

    @Test
    void testReRegistrationSwitchesOwner() {
        Object ownerA = new Object();
        Object ownerB = new Object();
        UUID player = UUID.randomUUID();

        SharedSelectionViewerRegistry.register(ownerA, player, 1);
        assertEquals(1, SharedSelectionViewerRegistry.getViewers(ownerA).size());

        // Same window re-registered under ownerB
        SharedSelectionViewerRegistry.register(ownerB, player, 1);

        assertTrue(SharedSelectionViewerRegistry.getViewers(ownerA).isEmpty());
        assertEquals(1, SharedSelectionViewerRegistry.getViewers(ownerB).size());
    }
}
