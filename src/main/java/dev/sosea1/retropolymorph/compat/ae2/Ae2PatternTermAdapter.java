package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/** Dependency-free adapter for AE2 UEL's Pattern Terminal crafting surface. */
public final class Ae2PatternTermAdapter implements RecipeSelectionAdapter {

    public static final Ae2PatternTermAdapter INSTANCE = new Ae2PatternTermAdapter();

    private Ae2PatternTermAdapter() {
    }

    @Override
    @Nullable
    public RecipeSelectionContext createContext(Container container) {
        if (!(container instanceof Ae2PatternTermExtension)) {
            return null;
        }

        Slot[] inputs = new Slot[9];
        Slot output = null;
        int inputCount = 0;

        for (Slot slot : container.inventorySlots) {
            if (slot instanceof Ae2PatternCraftingSlot) {
                int index = slot.getSlotIndex();
                if (index < 0 || index >= inputs.length || inputs[index] != null) {
                    return null;
                }
                inputs[index] = slot;
                inputCount++;
            }

            if (slot instanceof Ae2CraftingResultSlot) {
                if (output != null) {
                    return null;
                }
                output = slot;
            }
        }

        if (inputCount != inputs.length || output == null) {
            return null;
        }

        return new Ae2PatternTermContext(
                container,
                (Ae2PatternTermExtension) container,
                inputs,
                output);
    }
}
