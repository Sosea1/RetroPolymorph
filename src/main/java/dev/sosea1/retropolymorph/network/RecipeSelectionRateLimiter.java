package dev.sosea1.retropolymorph.network;

/**
 * Small per-player ingress limiter for selector packets.
 *
 * <p>Queries are more expensive and receive a tighter budget. Select/clear
 * actions share a higher total budget so normal wheel/keyboard cycling remains
 * responsive while a modified client cannot enqueue unlimited server-thread
 * recipe validations.</p>
 */
final class RecipeSelectionRateLimiter {

    static final int MAX_QUERIES_PER_SECOND = 20;
    static final int MAX_MESSAGES_PER_SECOND = 60;

    private long windowStartMs;
    private int queryCount;
    private int messageCount;

    synchronized boolean allow(boolean query) {
        return allow(query, System.currentTimeMillis());
    }

    synchronized boolean allow(boolean query, long nowMs) {
        if (this.windowStartMs == 0L
                || nowMs < this.windowStartMs
                || nowMs - this.windowStartMs >= 1000L) {
            this.windowStartMs = nowMs;
            this.queryCount = 0;
            this.messageCount = 0;
        }

        if (this.messageCount >= MAX_MESSAGES_PER_SECOND) {
            return false;
        }
        if (query && this.queryCount >= MAX_QUERIES_PER_SECOND) {
            return false;
        }

        this.messageCount++;
        if (query) {
            this.queryCount++;
        }
        return true;
    }
}
