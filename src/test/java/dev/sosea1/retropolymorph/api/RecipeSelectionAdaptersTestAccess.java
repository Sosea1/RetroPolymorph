package dev.sosea1.retropolymorph.api;

/** Test-only access to package-private adapter registry reset state. */
public final class RecipeSelectionAdaptersTestAccess {

    private RecipeSelectionAdaptersTestAccess() {
    }

    public static void reset() {
        RecipeSelectionAdapters.resetForTests();
    }
}
