package dev.sosea1.retropolymorph.compat.refinedstorage;

import net.minecraft.inventory.InventoryCrafting;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * One-shot invalidation bridge for Refined Storage's cached currentRecipe.
 *
 * NetworkNodeGrid intentionally keeps a matching recipe cached. That is good
 * for normal RS use, but an explicit Polymorph selection changes the desired
 * recipe without changing the matrix, so RS must be told to recompute once.
 */
public final class RefinedStorageSelectionRefresh {

    private static final Map<InventoryCrafting, Boolean> PENDING =
            Collections.synchronizedMap(new WeakHashMap<InventoryCrafting, Boolean>());

    private RefinedStorageSelectionRefresh() {
    }

    public static void request(InventoryCrafting matrix) {
        if (matrix != null) {
            PENDING.put(matrix, Boolean.TRUE);
        }
    }

    public static boolean consume(InventoryCrafting matrix) {
        return matrix != null && PENDING.remove(matrix) != null;
    }
}
