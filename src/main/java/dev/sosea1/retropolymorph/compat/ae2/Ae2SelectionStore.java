package dev.sosea1.retropolymorph.compat.ae2;

import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-container AE2 recipe selection.
 *
 * <p>The exact AE2 / wireless container class is deliberately not part of the
 * storage contract. Several 1.12.2 addons wrap or subclass the normal terminal
 * container, while the crafting slots remain AE2 slots. A weak identity-like
 * association lets the topology adapter support those containers without
 * keeping closed GUIs alive.</p>
 *
 * <p>Client and server own different Container instances, so their entries are
 * naturally isolated. On the server each player's open container has its own
 * selection, which also prevents cross-player recipe bleed.</p>
 */
public final class Ae2SelectionStore {

    private static final Map<Container, ResourceLocation> SELECTED =
            Collections.synchronizedMap(new WeakHashMap<Container, ResourceLocation>());

    private Ae2SelectionStore() {
    }

    public static void set(Container container, @Nullable ResourceLocation recipeId) {
        if (container == null) {
            return;
        }
        if (recipeId == null) {
            SELECTED.remove(container);
        } else {
            SELECTED.put(container, recipeId);
        }
    }

    @Nullable
    public static ResourceLocation get(Container container) {
        return container == null ? null : SELECTED.get(container);
    }

    public static void clear(Container container) {
        if (container != null) {
            SELECTED.remove(container);
        }
    }
}
