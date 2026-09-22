package dev.sosea1.retropolymorph.compat.rsb;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Accessor for Retro Sophisticated Backpacks matrix handlers, slot indexing, and selection storage.
 */
public final class RsbSlotAccess {

    private static final Map<IItemHandler, ResourceLocation> HANDLER_SELECTIONS =
            Collections.synchronizedMap(new WeakHashMap<IItemHandler, ResourceLocation>());

    private RsbSlotAccess() {
    }

    @Nullable
    public static ResourceLocation getStoredSelection(@Nullable IItemHandler handler) {
        return handler == null ? null : HANDLER_SELECTIONS.get(handler);
    }

    public static void storeSelection(@Nullable IItemHandler handler, @Nullable ResourceLocation id) {
        if (handler == null) {
            return;
        }
        if (id == null) {
            HANDLER_SELECTIONS.remove(handler);
        } else {
            HANDLER_SELECTIONS.put(handler, id);
        }
    }

    @Nullable
    public static IItemHandler extractMatrixHandler(@Nullable RsbCraftingAccess.ActiveCrafting active) {
        return RsbCraftingAccess.extractMatrixHandler(active);
    }

    public static void refreshClientMatrix(@Nullable RsbCraftingAccess.ActiveCrafting active) {
        RsbCraftingAccess.refreshClientMatrix(active);
    }
}
