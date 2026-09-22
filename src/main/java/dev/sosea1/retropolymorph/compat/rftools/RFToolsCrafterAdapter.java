package dev.sosea1.retropolymorph.compat.rftools;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/**
 * Adapter for RFTools Crafter containers.
 *
 * Checks for CrafterContainer without requiring hard compile-time dependencies.
 */
public final class RFToolsCrafterAdapter implements RecipeSelectionAdapter {

    public static final RFToolsCrafterAdapter INSTANCE = new RFToolsCrafterAdapter();

    private static final String CRAFTER_CONTAINER_CLASS =
            "mcjty.rftools.blocks.crafter.CrafterContainer";

    private RFToolsCrafterAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!container.getClass().getName().equals(CRAFTER_CONTAINER_CLASS)) {
            return AdapterDetectionResult.miss();
        }

        if (container.inventorySlots.size() < 10) {
            return AdapterDetectionResult.blockFallback();
        }

        Slot[] inputs = new Slot[9];
        for (int i = 0; i < 9; i++) {
            inputs[i] = container.inventorySlots.get(i);
        }

        Slot output = container.inventorySlots.get(9);
        if (output == null) {
            return AdapterDetectionResult.blockFallback();
        }

        return AdapterDetectionResult.match(new RFToolsCrafterContext(container, inputs, output));
    }
}
