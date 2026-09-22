package dev.sosea1.retropolymorph.api;

import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/**
 * Focused detector for one machine/processing recipe engine.
 *
 * <p>Recognition and surface creation are deliberately separate. Once a custom
 * engine is recognized, generic slot-guessing fallback is blocked even when the
 * surface cannot be opened safely.</p>
 */
public interface MachineRecipeAdapter {

    /** Cheap exact recognition of the owning custom engine/container. */
    boolean recognizes(Container container);

    /** Opens one live machine surface, or null when topology/state is unsafe. */
    @Nullable
    MachineRecipeSurface openSurface(Container container);
}
