package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
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
        if (container == null) {
            return null;
        }
        AdapterDetectionResult result = RecipeSelectionAdapters.probe(container);
        if (result.isMatch()) {
            return result.getContext();
        }
        if (result.isBlockFallback()) {
            return null;
        }

        SelectionContext crafting = CraftingContextDetector.detect(container);
        SelectionContext furnace = FurnaceContextDetector.detect(container);

        if (crafting != null && furnace != null) {
            return null;
        }
        return crafting != null ? crafting : furnace;
    }
}
