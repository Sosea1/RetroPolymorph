package dev.sosea1.retropolymorph.api;

import net.minecraft.inventory.Container;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Lock-free read registry. Registration is expected during startup; detection
 * happens on GUI open and selection packets, so reads are kept allocation-free.
 */
public final class RecipeSelectionAdapters {

    private static final RecipeSelectionAdapter[] EMPTY = new RecipeSelectionAdapter[0];

    private static volatile RecipeSelectionAdapter[] adapters = EMPTY;

    private RecipeSelectionAdapters() {
    }

    public static synchronized void register(RecipeSelectionAdapter adapter) {
        if (adapter == null) {
            throw new NullPointerException("adapter");
        }

        RecipeSelectionAdapter[] current = adapters;
        RecipeSelectionAdapter[] updated = Arrays.copyOf(current, current.length + 1);
        updated[current.length] = adapter;
        adapters = updated;
    }

    @Nullable
    public static SelectionContext detect(Container container) {
        RecipeSelectionAdapter[] snapshot = adapters;
        for (RecipeSelectionAdapter adapter : snapshot) {
            SelectionContext context = adapter.createContext(container);
            if (context != null) {
                return context;
            }
        }
        return null;
    }
}
