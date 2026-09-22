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
        AdapterDetectionResult result = probe(container);
        return result.isMatch() ? result.getContext() : null;
    }

    /**
     * Preserves whether a recognized adapter is temporarily blocking generic
     * fallback, so callers that cache misses do not turn that state permanent.
     */
    public static AdapterDetectionResult probe(Container container) {
        if (container == null) {
            return AdapterDetectionResult.miss();
        }
        AdapterDetectionResult result = RecipeSelectionAdapters.probe(container);
        if (result.isMatch()) {
            return result;
        }
        if (result.isBlockFallback()) {
            return result;
        }

        SelectionContext crafting = CraftingContextDetector.detect(container);
        SelectionContext furnace = FurnaceContextDetector.detect(container);

        if (crafting != null && furnace != null) {
            return AdapterDetectionResult.blockFallback();
        }
        SelectionContext context = crafting != null ? crafting : furnace;
        return context == null ? AdapterDetectionResult.miss() : AdapterDetectionResult.match(context);
    }
}
