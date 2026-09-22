package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.mixin.SlotCraftingAccessor;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Conservative generic detector for containers that expose a real
 * InventoryCrafting through their slots.
 *
 * The exact SlotCrafting -> craftMatrix link is preferred because it proves the
 * result slot and matrix belong together. The looser inventory-topology path is
 * only a fallback for custom result-slot classes. Ambiguous layouts are rejected
 * instead of guessing.
 *
 * No reflection and no class-name guesses live here. Custom engines belong in
 * focused adapters.
 */
public final class CraftingContextDetector {

    private CraftingContextDetector() {
    }

    @Nullable
    public static RecipeSelectionContext detect(Container container) {
        if (container == null) {
            return null;
        }

        CraftingContext exact = detectFromSlotCrafting(container);
        if (exact != null) {
            return exact;
        }

        if (hasSlotCraftingAccessor(container)) {
            return null;
        }

        return detectFromInventoryTopology(container);
    }

    @Nullable
    private static CraftingContext detectFromSlotCrafting(Container container) {
        Slot candidateSlot = null;
        InventoryCrafting candidateMatrix = null;

        for (Slot slot : container.inventorySlots) {
            if (!(slot instanceof SlotCrafting) || !(slot instanceof SlotCraftingAccessor)) {
                continue;
            }

            InventoryCrafting matrix = ((SlotCraftingAccessor) slot).retropolymorph$getCraftMatrix();
            if (candidateSlot == null) {
                candidateSlot = slot;
                candidateMatrix = matrix;
                continue;
            }

            if (candidateMatrix != matrix) {
                return null;
            }
        }

        return candidateSlot == null
                ? null
                : new CraftingContext(container, candidateMatrix, candidateSlot);
    }

    private static boolean hasSlotCraftingAccessor(Container container) {
        for (Slot slot : container.inventorySlots) {
            if (slot instanceof SlotCrafting && slot instanceof SlotCraftingAccessor) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static CraftingContext detectFromInventoryTopology(Container container) {
        IdentityHashMap<InventoryCrafting, Integer> referenceCounts =
                new IdentityHashMap<InventoryCrafting, Integer>();

        Slot resultSlot = null;
        boolean ambiguousResult = false;

        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof InventoryCrafting) {
                InventoryCrafting matrix = (InventoryCrafting) slot.inventory;
                Integer count = referenceCounts.get(matrix);
                referenceCounts.put(matrix, count == null ? 1 : count + 1);
            }

            if (slot.inventory instanceof InventoryCraftResult) {
                if (resultSlot == null) {
                    resultSlot = slot;
                } else if (resultSlot != slot) {
                    ambiguousResult = true;
                }
            }
        }

        if (resultSlot == null || ambiguousResult || referenceCounts.isEmpty()) {
            return null;
        }

        InventoryCrafting bestMatrix = null;
        int bestReferences = 0;
        boolean ambiguousMatrix = false;

        for (Map.Entry<InventoryCrafting, Integer> entry : referenceCounts.entrySet()) {
            int references = entry.getValue();
            if (references > bestReferences) {
                bestReferences = references;
                bestMatrix = entry.getKey();
                ambiguousMatrix = false;
            } else if (references == bestReferences) {
                ambiguousMatrix = true;
            }
        }

        if (bestMatrix == null || ambiguousMatrix) {
            return null;
        }

        return new CraftingContext(container, bestMatrix, resultSlot);
    }
}
