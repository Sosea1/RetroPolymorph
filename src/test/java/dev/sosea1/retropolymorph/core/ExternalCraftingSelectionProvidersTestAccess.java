package dev.sosea1.retropolymorph.core;

/** Test-only access to package-private external provider registry reset state. */
public final class ExternalCraftingSelectionProvidersTestAccess {

    private ExternalCraftingSelectionProvidersTestAccess() {
    }

    public static void reset() {
        ExternalCraftingSelectionProviders.resetForTests();
    }
}
