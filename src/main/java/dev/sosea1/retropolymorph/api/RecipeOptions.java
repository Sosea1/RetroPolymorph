package dev.sosea1.retropolymorph.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared limits and transport sanitization for recipe-selection options. */
public final class RecipeOptions {

    /** Mirrors modern Polymorph's widget cap. */
    public static final int MAX_SERVER_OPTIONS = 15;

    private RecipeOptions() {
    }

    public static List<RecipeOption> sanitizeAndLimit(
            List<RecipeOption> options,
            String selectedRecipeKey) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<RecipeOption> safe = new ArrayList<RecipeOption>(
                Math.min(options.size(), MAX_SERVER_OPTIONS));
        Set<String> seenKeys = new HashSet<String>();
        RecipeOption selectedOutsideWindow = null;

        for (RecipeOption option : options) {
            if (option == null
                    || !RecipeKey.isWireSafe(option.getRecipeKey())
                    || option.getOutput().isEmpty()
                    || !seenKeys.add(option.getRecipeKey())) {
                continue;
            }

            if (safe.size() < MAX_SERVER_OPTIONS) {
                safe.add(option);
                continue;
            }

            if (selectedRecipeKey != null
                    && selectedRecipeKey.equals(option.getRecipeKey())) {
                selectedOutsideWindow = option;
                break;
            }
        }

        if (selectedOutsideWindow != null) {
            if (safe.size() == MAX_SERVER_OPTIONS) {
                safe.set(MAX_SERVER_OPTIONS - 1, selectedOutsideWindow);
            } else {
                safe.add(selectedOutsideWindow);
            }
        }

        if (safe.size() <= 1) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(safe);
    }
}
