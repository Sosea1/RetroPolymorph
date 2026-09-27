package dev.sosea1.retropolymorph.client;

/** A bounded client-only visual/input gate while a new template is being resolved. */
final class ClientResultMask {

    private static final long MAX_PENDING_NANOS = 5_000_000_000L;

    private ClientResultMask() {
    }

    static boolean shouldMask(
            boolean resultSlot,
            boolean hasInputs,
            boolean awaitingSnapshot,
            long pendingSinceNanos,
            long nowNanos) {
        return resultSlot
                && hasInputs
                && awaitingSnapshot
                && pendingSinceNanos > 0L
                && nowNanos - pendingSinceNanos >= 0L
                && nowNanos - pendingSinceNanos < MAX_PENDING_NANOS;
    }
}
