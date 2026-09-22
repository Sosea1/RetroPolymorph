package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.network.NetworkHandler;

/**
 * Coalesces client-side recipe selection queries occurring within the same client tick.
 *
 * <p>During rapid crafting or multiple slot invalidations in a single client frame/tick,
 * this scheduler coalesces query signals and sends exactly one authoritative query for
 * the latest {@code inputRevision} at tick end. {@code SELECT} and {@code CLEAR} actions
 * bypass this scheduler and remain strictly immediate.</p>
 */
public final class SelectorQueryScheduler {

    private static boolean hasPending;
    private static int pendingWindowId;
    private static int pendingSessionToken;
    private static int pendingInputRevision;

    private SelectorQueryScheduler() {
    }

    /**
     * Schedules a query for the specified container window and input revision.
     * If a query is already pending for this window, the revision is updated.
     */
    public static synchronized void scheduleQuery(int windowId, int sessionToken, int inputRevision) {
        pendingWindowId = windowId;
        pendingSessionToken = sessionToken;
        pendingInputRevision = inputRevision;
        hasPending = true;
    }

    /**
     * Cancels any pending query. Called when GUI is closed or window changes.
     */
    public static synchronized void cancel() {
        hasPending = false;
    }

    /**
     * Flushes any pending query immediately.
     */
    public static synchronized void flushNow() {
        if (!hasPending) {
            return;
        }
        int windowId = pendingWindowId;
        int sessionToken = pendingSessionToken;
        int revision = pendingInputRevision;
        hasPending = false;
        NetworkHandler.query(windowId, sessionToken, revision);
    }

    /**
     * Called at the end of each client tick (ClientTickEvent.Phase.END) to dispatch
     * the coalesced query.
     */
    public static synchronized void onTickEnd() {
        flushNow();
    }

    public static synchronized boolean hasPendingQuery() {
        return hasPending;
    }

    public static synchronized int getPendingInputRevision() {
        return pendingInputRevision;
    }

    public static synchronized int getPendingWindowId() {
        return pendingWindowId;
    }

    public static synchronized int getPendingSessionToken() {
        return pendingSessionToken;
    }

    static synchronized void resetForTests() {
        hasPending = false;
        pendingWindowId = 0;
        pendingSessionToken = 0;
        pendingInputRevision = 0;
    }
}
