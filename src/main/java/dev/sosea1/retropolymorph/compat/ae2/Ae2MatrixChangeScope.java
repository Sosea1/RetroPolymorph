package dev.sosea1.retropolymorph.compat.ae2;

import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/**
 * Thread-local scope carrying the active AE2 container through matrix change
 * callbacks.
 *
 * <p>AE2 evaluates recipes against a scratch {@code InventoryCrafting} whose
 * event handler is an unowned {@code ContainerNull}. This scope bridges that
 * scratch matrix back to the originating terminal container so CraftingManager
 * lookups receive the correct player/container selection.</p>
 */
public final class Ae2MatrixChangeScope {

    private static final ThreadLocal<Container> CURRENT = new ThreadLocal<Container>();

    private Ae2MatrixChangeScope() {
    }

    public static void enter(Container container) {
        CURRENT.set(container);
    }

    public static void exit() {
        CURRENT.remove();
    }

    @Nullable
    public static Container currentContainer() {
        return CURRENT.get();
    }
}
