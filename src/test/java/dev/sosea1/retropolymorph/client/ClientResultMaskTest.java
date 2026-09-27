package dev.sosea1.retropolymorph.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientResultMaskTest {

    @Test
    void onlyPendingResultIsMaskedAndTimeoutFailsOpen() {
        long started = 1_000_000_000L;
        assertTrue(ClientResultMask.shouldMask(true, true, true, started, started + 100_000_000L));
        assertFalse(ClientResultMask.shouldMask(false, true, true, started, started + 100_000_000L));
        assertFalse(ClientResultMask.shouldMask(true, false, true, started, started + 100_000_000L));
        assertFalse(ClientResultMask.shouldMask(true, true, false, started, started + 100_000_000L));
        assertFalse(ClientResultMask.shouldMask(true, true, true, started, started + 5_000_000_001L));
    }
}
