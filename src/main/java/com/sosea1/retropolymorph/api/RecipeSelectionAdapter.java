package com.sosea1.retropolymorph.api;

import net.minecraft.inventory.Container;

/**
 * Compatibility hook for focused recipe-selection surfaces that do
 * not fit generic crafting/furnace detectors.
 *
 * <p>Adapters should be cheap to probe and return {@link AdapterDetectionResult#miss()}
 * for unrelated containers.</p>
 */
@FunctionalInterface
public interface RecipeSelectionAdapter {

    /**
     * Probes this container and returns MATCH with context,
     * BLOCK_FALLBACK to suppress generic detection, or MISS.
     *
     * @param container the open container to inspect
     * @return detection result; never null
     */
    AdapterDetectionResult probe(Container container);
}
