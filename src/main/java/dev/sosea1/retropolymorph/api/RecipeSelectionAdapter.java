package dev.sosea1.retropolymorph.api;

import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/**
 * Optional compatibility hook for focused recipe-selection surfaces that do
 * not fit the generic crafting/furnace detectors.
 *
 * Adapters should be cheap to probe and return null for unrelated containers.
 */
public interface RecipeSelectionAdapter {

    @Nullable
    SelectionContext createContext(Container container);
}
