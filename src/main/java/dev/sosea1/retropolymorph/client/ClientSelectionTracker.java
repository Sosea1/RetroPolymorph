package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;

import javax.annotation.Nullable;
import java.util.List;

public final class ClientSelectionTracker {

    private static int windowId = -1;
    private static int sessionToken;
    private static int nextSessionToken = 1;

    @Nullable
    private static String selectedRecipeKey;

    private static boolean lastAccepted = true;
    private static boolean reconciliationQueryPending;

    private static long revision;

    private ClientSelectionTracker() {
    }

    public static int begin(int currentWindowId) {
        windowId = currentWindowId;
        sessionToken = nextToken();
        selectedRecipeKey = null;
        lastAccepted = true;
        reconciliationQueryPending = false;
        return sessionToken;
    }

    public static void reset() {
        windowId = -1;
        sessionToken = 0;
        selectedRecipeKey = null;
        lastAccepted = true;
        reconciliationQueryPending = false;
    }

    public static void apply(
            int responseWindowId,
            int responseSessionToken,
            boolean accepted,
            @Nullable String selectedRecipe) {
        if (!matches(responseWindowId, responseSessionToken)) {
            return;
        }

        selectedRecipeKey = selectedRecipe;
        lastAccepted = accepted;
        reconciliationQueryPending = false;
        revision++;
    }

    @Nullable
    static String getSelectedRecipeKey(int currentWindowId, int currentSessionToken) {
        return matches(currentWindowId, currentSessionToken) ? selectedRecipeKey : null;
    }

    static boolean wasLastAccepted(int currentWindowId, int currentSessionToken) {
        return !matches(currentWindowId, currentSessionToken) || lastAccepted;
    }

    static long getRevision(int currentWindowId, int currentSessionToken) {
        return matches(currentWindowId, currentSessionToken) ? revision : -1L;
    }

    static boolean reconcile(
            int currentWindowId,
            int currentSessionToken,
            List<RecipeOption> choices,
            boolean retainWhenEmpty) {
        if (!matches(currentWindowId, currentSessionToken)) {
            return false;
        }
        if (selectedRecipeKey == null) {
            reconciliationQueryPending = false;
            return false;
        }
        if (choices.isEmpty() && retainWhenEmpty) {
            return false;
        }

        for (RecipeOption choice : choices) {
            if (selectedRecipeKey.equals(choice.getRecipeKey())) {
                reconciliationQueryPending = false;
                return false;
            }
        }

        if (reconciliationQueryPending) {
            return false;
        }
        reconciliationQueryPending = true;
        return true;
    }

    private static boolean matches(int currentWindowId, int currentSessionToken) {
        return windowId == currentWindowId && sessionToken == currentSessionToken && currentWindowId >= 0;
    }

    private static int nextToken() {
        int token = nextSessionToken++;
        if (nextSessionToken == Integer.MAX_VALUE) {
            nextSessionToken = 1;
        }
        return token;
    }
}
