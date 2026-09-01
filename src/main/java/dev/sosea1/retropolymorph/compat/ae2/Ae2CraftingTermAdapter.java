package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/**
 * Dependency-free AE2 UEL crafting-terminal adapter. Optional Mixins add the
 * marker/bridge interfaces only when the corresponding AE2 classes exist.
 */
public final class Ae2CraftingTermAdapter implements RecipeSelectionAdapter {

    public static final Ae2CraftingTermAdapter INSTANCE = new Ae2CraftingTermAdapter();

    private Ae2CraftingTermAdapter() {
    }

    @Override
    @Nullable
    public RecipeSelectionContext createContext(Container container) {
        if (!(container instanceof Ae2CraftingTermExtension)) {
            return null;
        }

        Slot[] inputs = new Slot[9];
        Slot output = null;
        int inputCount = 0;

        for (Slot slot : container.inventorySlots) {
            if (slot instanceof Ae2CraftingMatrixSlot) {
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

        return new Ae2CraftingTermContext(
                container,
                (Ae2CraftingTermExtension) container,
                inputs,
                output);
    }
}
