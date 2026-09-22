package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionReason;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Tracks one GUI lifetime and rejects stale server snapshots by input revision. */
public final class ClientSelectionTracker {

    private static int windowId = -1;
    private static int sessionToken;
    private static int nextSessionToken = 1;
    private static int expectedInputRevision;

    @Nullable
    private static String selectedRecipeKey;

    private static boolean lastAccepted = true;
    private static boolean snapshotReceived;
    private static List<RecipeOption> options = Collections.emptyList();
    private static SelectionReason lastReason = SelectionReason.NATIVE_DEFAULT;
    private static long revision;

    private ClientSelectionTracker() {
    }

    public static int begin(int currentWindowId) {
        windowId = currentWindowId;
        sessionToken = nextToken();
        expectedInputRevision = 0;
        selectedRecipeKey = null;
        lastAccepted = true;
        snapshotReceived = false;
        options = Collections.emptyList();
        lastReason = SelectionReason.NATIVE_DEFAULT;
        return sessionToken;
    }

    public static void reset() {
        windowId = -1;
        sessionToken = 0;
        expectedInputRevision = 0;
        selectedRecipeKey = null;
        lastAccepted = true;
        snapshotReceived = false;
        options = Collections.emptyList();
        lastReason = SelectionReason.NATIVE_DEFAULT;
    }

    /**
     * Rebinds the currently active GUI session to the container's live window id.
     *
     * <p>Forge/Cleanroom can initialize a client GUI while its container still carries
     * the provisional window id (commonly {@code 0}), then assign the server window id
     * shortly afterwards. The session token is the GUI-lifetime identity; the window id
     * is allowed to change inside that same session. A rebind drops any snapshot tied to
     * the provisional id so it cannot leak into the live container.</p>
     *
     * @return {@code true} only when the active session was rebound to a different id
     */
    static boolean rebindWindowId(int currentWindowId, int currentSessionToken) {
        if (currentWindowId < 0
                || currentSessionToken == 0
                || sessionToken != currentSessionToken
                || windowId == currentWindowId) {
            return false;
        }

        windowId = currentWindowId;
        expectedInputRevision = 0;
        selectedRecipeKey = null;
        lastAccepted = true;
        snapshotReceived = false;
        options = Collections.emptyList();
        lastReason = SelectionReason.NATIVE_DEFAULT;
        return true;
    }

    static void expectInputRevision(
            int currentWindowId,
            int currentSessionToken,
            int inputRevision) {
        if (!matches(currentWindowId, currentSessionToken)) {
            return;
        }
        expectedInputRevision = inputRevision;
        snapshotReceived = false;
        options = Collections.emptyList();
    }

    public static void apply(
            int responseWindowId,
            int responseSessionToken,
            int responseInputRevision,
            boolean accepted,
            @Nullable String selectedRecipe,
            List<RecipeOption> serverOptions,
            SelectionReason reason) {
        if (!matches(responseWindowId, responseSessionToken)
                || responseInputRevision != expectedInputRevision) {
            return;
        }

        selectedRecipeKey = selectedRecipe;
        lastAccepted = accepted;
        snapshotReceived = true;
        options = serverOptions == null
                ? Collections.<RecipeOption>emptyList()
                : serverOptions;
        lastReason = reason == null ? SelectionReason.NATIVE_DEFAULT : reason;
        revision++;
    }

    public static void apply(
            int responseWindowId,
            int responseSessionToken,
            int responseInputRevision,
            boolean accepted,
            @Nullable String selectedRecipe,
            List<RecipeOption> serverOptions) {
        apply(responseWindowId, responseSessionToken, responseInputRevision, accepted, selectedRecipe, serverOptions, SelectionReason.NATIVE_DEFAULT);
    }

    static void applyOptimisticSelection(
            int currentWindowId,
            int currentSessionToken,
            @Nullable String selectedRecipe) {
        if (!matches(currentWindowId, currentSessionToken)) {
            return;
        }
        selectedRecipeKey = selectedRecipe;
        lastAccepted = true;
        lastReason = SelectionReason.PLAYER_SELECTION;
    }

    @Nullable
    static String getSelectedRecipeKey(int currentWindowId, int currentSessionToken) {
        return matches(currentWindowId, currentSessionToken) ? selectedRecipeKey : null;
    }

    static SelectionReason getLastReason(int currentWindowId, int currentSessionToken) {
        return matches(currentWindowId, currentSessionToken) ? lastReason : SelectionReason.NATIVE_DEFAULT;
    }

    public static SelectionReason getLastReason() {
        return lastReason;
    }

    /**
     * Compatibility hook for client GUI actions that need to carry the last
     * server-authoritative choice through a third-party packet. The window id
     * prevents a stale choice from a previous container being reused.
     */
    @Nullable
    public static String getSelectedRecipeKeyForWindow(int currentWindowId) {
        return windowId == currentWindowId && currentWindowId >= 0
                ? selectedRecipeKey
                : null;
    }

    public static boolean hasAuthoritativeSnapshotForWindow(int currentWindowId) {
        return windowId == currentWindowId
                && currentWindowId >= 0
                && snapshotReceived;
    }

    static boolean wasLastAccepted(int currentWindowId, int currentSessionToken) {
        return !matches(currentWindowId, currentSessionToken) || lastAccepted;
    }

    static long getRevision(int currentWindowId, int currentSessionToken) {
        return matches(currentWindowId, currentSessionToken) ? revision : -1L;
    }

    static List<RecipeOption> getOptions(int currentWindowId, int currentSessionToken) {
        return matches(currentWindowId, currentSessionToken)
                ? options
                : Collections.<RecipeOption>emptyList();
    }

    private static boolean matches(int currentWindowId, int currentSessionToken) {
        return windowId == currentWindowId
                && sessionToken == currentSessionToken
                && currentWindowId >= 0;
    }

    private static int nextToken() {
        int token = nextSessionToken++;
        if (nextSessionToken == Integer.MAX_VALUE) {
            nextSessionToken = 1;
        }
        return token;
    }
}
