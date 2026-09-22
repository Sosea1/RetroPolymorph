package dev.sosea1.retropolymorph.compat.fastsuite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FastSuiteInteropTest {

    @Test
    public void currentCandidateApiIsDetectedWithoutCheckingAModId() {
        assertTrue(FastSuiteInterop.isInstalled());
    }
}
