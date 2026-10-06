package com.sosea1.retropolymorph.compat.avaritia;

import com.sosea1.retropolymorph.api.MachineRecipeAdapter;
import com.sosea1.retropolymorph.api.MachineRecipeSurface;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

public final class AvaritiaExtremeCraftingAdapter implements MachineRecipeAdapter {

    public static final AvaritiaExtremeCraftingAdapter INSTANCE =
            new AvaritiaExtremeCraftingAdapter();

    private AvaritiaExtremeCraftingAdapter() {
    }

    @Override
    public boolean recognizes(Container container) {
        return AvaritiaExtremeCraftingReflection.isContainer(container);
    }

    @Override
    @Nullable
    public MachineRecipeSurface openSurface(Container container) {
        AvaritiaExtremeCraftingReflection.Binding binding =
                AvaritiaExtremeCraftingReflection.bind(container);
        return binding == null
                ? null
                : new AvaritiaExtremeCraftingSurface(container, binding);
    }
}
