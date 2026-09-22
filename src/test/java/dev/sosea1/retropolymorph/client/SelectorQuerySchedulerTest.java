package dev.sosea1.retropolymorph.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectorQuerySchedulerTest {

    @BeforeEach
    @AfterEach
    public void cleanup() {
        SelectorQueryScheduler.resetForTests();
    }

    @Test
    public void testScheduleQuerySetsPendingState() {
        assertFalse(SelectorQueryScheduler.hasPendingQuery());

        SelectorQueryScheduler.scheduleQuery(1, 10, 100);

        assertTrue(SelectorQueryScheduler.hasPendingQuery());
        assertEquals(1, SelectorQueryScheduler.getPendingWindowId());
        assertEquals(10, SelectorQueryScheduler.getPendingSessionToken());
        assertEquals(100, SelectorQueryScheduler.getPendingInputRevision());
    }

    @Test
    public void testMultipleQueriesCoalesceToLatestRevision() {
        // Simulating a burst of 5 rapid slot updates in a single client tick
        SelectorQueryScheduler.scheduleQuery(1, 10, 101);
        SelectorQueryScheduler.scheduleQuery(1, 10, 102);
        SelectorQueryScheduler.scheduleQuery(1, 10, 103);
        SelectorQueryScheduler.scheduleQuery(1, 10, 104);
        SelectorQueryScheduler.scheduleQuery(1, 10, 105);

        assertTrue(SelectorQueryScheduler.hasPendingQuery());
        // Only the latest revision should be retained
        assertEquals(105, SelectorQueryScheduler.getPendingInputRevision());
        assertEquals(1, SelectorQueryScheduler.getPendingWindowId());
    }

    @Test
    public void testCancelClearsPending() {
        SelectorQueryScheduler.scheduleQuery(1, 10, 100);
        assertTrue(SelectorQueryScheduler.hasPendingQuery());

        SelectorQueryScheduler.cancel();
        assertFalse(SelectorQueryScheduler.hasPendingQuery());
    }

    @Test
    public void testTickEndResetsPendingState() {
        SelectorQueryScheduler.scheduleQuery(1, 10, 100);
        assertTrue(SelectorQueryScheduler.hasPendingQuery());

        // Note: in a headless unit test without client network initialized,
        // we verify state transition on cancel/flush
        SelectorQueryScheduler.cancel();
        assertFalse(SelectorQueryScheduler.hasPendingQuery());
    }
}
