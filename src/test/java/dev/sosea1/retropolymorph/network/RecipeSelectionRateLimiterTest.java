package dev.sosea1.retropolymorph.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RecipeSelectionRateLimiterTest {

    @Test
    public void queriesUseTighterBudget() {
        RecipeSelectionRateLimiter limiter = new RecipeSelectionRateLimiter();
        for (int index = 0; index < RecipeSelectionRateLimiter.MAX_QUERIES_PER_SECOND; index++) {
            assertTrue(limiter.allow(true, 100L));
        }
        assertFalse(limiter.allow(true, 100L));
        assertTrue(limiter.allow(true, 1100L));
    }

    @Test
    public void actionsUseSharedTotalBudget() {
        RecipeSelectionRateLimiter limiter = new RecipeSelectionRateLimiter();
        for (int index = 0; index < RecipeSelectionRateLimiter.MAX_MESSAGES_PER_SECOND; index++) {
            assertTrue(limiter.allow(false, 200L));
        }
        assertFalse(limiter.allow(false, 200L));
        assertFalse(limiter.allow(true, 200L));
    }

    @Test
    public void rejectedQueryDoesNotConsumeRemainingActionBudget() {
        RecipeSelectionRateLimiter limiter = new RecipeSelectionRateLimiter();
        for (int index = 0; index < RecipeSelectionRateLimiter.MAX_QUERIES_PER_SECOND; index++) {
            assertTrue(limiter.allow(true, 300L));
        }
        assertFalse(limiter.allow(true, 300L));
        for (int index = RecipeSelectionRateLimiter.MAX_QUERIES_PER_SECOND;
                index < RecipeSelectionRateLimiter.MAX_MESSAGES_PER_SECOND; index++) {
            assertTrue(limiter.allow(false, 300L));
        }
        assertFalse(limiter.allow(false, 300L));
    }

    @Test
    public void backwardsClockStartsFreshWindow() {
        RecipeSelectionRateLimiter limiter = new RecipeSelectionRateLimiter();
        for (int index = 0; index < RecipeSelectionRateLimiter.MAX_MESSAGES_PER_SECOND; index++) {
            assertTrue(limiter.allow(false, 5000L));
        }
        assertTrue(limiter.allow(false, 4000L));
    }
}
