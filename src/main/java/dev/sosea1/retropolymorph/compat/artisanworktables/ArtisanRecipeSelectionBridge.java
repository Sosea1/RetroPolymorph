package dev.sosea1.retropolymorph.compat.artisanworktables;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Thread-local virtual recipe ordering for Artisan's native registry lookup. */
public final class ArtisanRecipeSelectionBridge {

    private enum Source {
        PROBE,
        SELECTION
    }

    private static final ThreadLocal<State> STATE = new ThreadLocal<State>();

    private ArtisanRecipeSelectionBridge() {
    }

    static boolean isActive() {
        return STATE.get() != null;
    }

    static boolean isProbeActive() {
        State state = STATE.get();
        return state != null && state.source == Source.PROBE;
    }

    static void beginProbe(Object recipe) {
        STATE.set(new State(Source.PROBE, recipe, null, null));
    }

    static void endProbe() {
        State state = STATE.get();
        if (state != null && state.source == Source.PROBE) {
            STATE.remove();
        }
    }

    static void beginSelection(Object recipe, Object tile, UUID playerId) {
        STATE.set(new State(Source.SELECTION, recipe, tile, playerId));
    }

    static boolean isSelection(Object tile, UUID playerId) {
        State state = STATE.get();
        return state != null
                && state.source == Source.SELECTION
                && state.tile == tile
                && state.playerId != null
                && state.playerId.equals(playerId);
    }

    @Nullable
    static Object forcedRecipe() {
        State state = STATE.get();
        return state == null ? null : state.recipe;
    }

    static void endSelection() {
        State state = STATE.get();
        if (state != null && state.source == Source.SELECTION) {
            STATE.remove();
        }
    }

    public static Object get(List<?> recipes, int virtualIndex) {
        return recipes.get(mapIndex(recipes, virtualIndex));
    }

    public static Object remove(List<?> recipes, int virtualIndex) {
        return recipes.remove(mapIndex(recipes, virtualIndex));
    }

    private static int mapIndex(List<?> recipes, int virtualIndex) {
        Object selected = forcedRecipe();
        if (selected == null || recipes == null || virtualIndex < 0 || virtualIndex >= recipes.size()) {
            return virtualIndex;
        }

        int selectedIndex = identityIndexOf(recipes, selected);
        if (selectedIndex < 0 || selectedIndex == recipes.size() - 1) {
            return virtualIndex;
        }
        if (virtualIndex == recipes.size() - 1) {
            return selectedIndex;
        }
        return virtualIndex < selectedIndex ? virtualIndex : virtualIndex + 1;
    }

    private static int identityIndexOf(List<?> recipes, Object selected) {
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index) == selected) {
                return index;
            }
        }
        return -1;
    }

    private static final class State {
        final Source source;
        final Object recipe;
        final Object tile;
        final UUID playerId;

        private State(Source source, Object recipe, Object tile, UUID playerId) {
            this.source = source;
            this.recipe = recipe;
            this.tile = tile;
            this.playerId = playerId;
        }
    }
}
