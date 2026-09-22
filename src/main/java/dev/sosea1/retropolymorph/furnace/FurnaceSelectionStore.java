package dev.sosea1.retropolymorph.furnace;

import net.minecraft.inventory.IInventory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared per-inventory state used by the GUI, furnace tick hook and XP hook.
 * A weak key avoids retaining an unloaded tile entity solely because a player
 * once selected one of its recipes.
 */
public final class FurnaceSelectionStore {

    private static final Map<IInventory, FurnaceSelectionState> STATES =
            new WeakHashMap<IInventory, FurnaceSelectionState>();

    private FurnaceSelectionStore() {
    }

    @Nullable
    public static synchronized FurnaceSelectionState peek(IInventory inventory) {
        return STATES.get(inventory);
    }

    public static synchronized FurnaceSelectionState getOrCreate(IInventory inventory) {
        FurnaceSelectionState state = STATES.get(inventory);
        if (state == null) {
            state = new FurnaceSelectionState();
            STATES.put(inventory, state);
        }
        return state;
    }
}
