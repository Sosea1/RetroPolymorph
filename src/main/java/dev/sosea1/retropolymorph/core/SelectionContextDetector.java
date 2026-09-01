package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapters;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.furnace.FurnaceContextDetector;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/** Detects exactly one supported selection surface in an open container. */
public final class SelectionContextDetector {

    private SelectionContextDetector() {
    }

    @Nullable
    public static SelectionContext detect(Container container) {
        SelectionContext adapted = RecipeSelectionAdapters.detect(container);
        if (adapted != null) {
            return adapted;
        }

        SelectionContext crafting = CraftingContextDetector.detect(container);
        SelectionContext furnace = FurnaceContextDetector.detect(container);

        if (crafting != null && furnace != null) {
            return null;
        }
        return crafting != null ? crafting : furnace;
    }
}
