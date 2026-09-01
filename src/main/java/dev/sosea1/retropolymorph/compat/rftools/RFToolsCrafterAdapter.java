package dev.sosea1.retropolymorph.compat.rftools;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
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
    @Nullable
    public RecipeSelectionContext createContext(Container container) {
        if (!container.getClass().getName().equals(CRAFTER_CONTAINER_CLASS)) {
            return null;
        }

        if (container.inventorySlots.size() < 10) {
            return null;
        }

        // Slots 0..8 are the 3x3 ghost crafting input slots
        Slot[] inputs = new Slot[9];
        for (int i = 0; i < 9; i++) {
            inputs[i] = container.inventorySlots.get(i);
        }

        // Slot 9 is the ghost crafting output slot
        Slot output = container.inventorySlots.get(9);
        if (output == null) {
            return null;
        }

        return new RFToolsCrafterContext(container, inputs, output);
    }
}
