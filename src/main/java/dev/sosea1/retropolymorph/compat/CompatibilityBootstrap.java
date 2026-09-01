package dev.sosea1.retropolymorph.compat;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapters;
import dev.sosea1.retropolymorph.compat.ae2.Ae2CraftingTermAdapter;
import dev.sosea1.retropolymorph.compat.ae2.Ae2PatternTermAdapter;
import dev.sosea1.retropolymorph.compat.extendedcrafting.ExtendedTableAdapter;

/** Registers built-in compatibility adapters. */
public final class CompatibilityBootstrap {

    private static boolean initialized;

    private CompatibilityBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        RecipeSelectionAdapters.register(Ae2PatternTermAdapter.INSTANCE);
        RecipeSelectionAdapters.register(Ae2CraftingTermAdapter.INSTANCE);
        RecipeSelectionAdapters.register(ExtendedTableAdapter.INSTANCE);
        RecipeSelectionAdapters.register(dev.sosea1.retropolymorph.compat.rftools.RFToolsCrafterAdapter.INSTANCE);
        initialized = true;
    }
}
