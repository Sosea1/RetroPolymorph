package dev.sosea1.retropolymorph.compat.extrautils2;

import dev.sosea1.retropolymorph.api.MachineRecipeAdapter;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/** Focused machine-API adapter for Extra Utilities 2 Mechanical/Analog Crafter. */
public final class ExtraUtilities2CrafterAdapter implements MachineRecipeAdapter {

    public static final ExtraUtilities2CrafterAdapter INSTANCE = new ExtraUtilities2CrafterAdapter();

    private ExtraUtilities2CrafterAdapter() {
    }

    @Override
    public boolean recognizes(Container container) {
        return ExtraUtilities2CrafterReflection.recognizes(container);
    }

    @Override
    @Nullable
    public MachineRecipeSurface openSurface(Container container) {
        ExtraUtilities2CrafterReflection.Resolved resolved =
                ExtraUtilities2CrafterReflection.resolve(container);
        return resolved == null ? null : new ExtraUtilities2CrafterSurface(container, resolved);
    }
}
