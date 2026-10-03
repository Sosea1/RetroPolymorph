package dev.sosea1.retropolymorph.compat.artisanworktables;

import dev.sosea1.retropolymorph.api.MachineRecipeAdapter;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

public final class ArtisanWorktableAdapter implements MachineRecipeAdapter {

    public static final ArtisanWorktableAdapter INSTANCE = new ArtisanWorktableAdapter();

    private ArtisanWorktableAdapter() {
    }

    @Override
    public boolean recognizes(Container container) {
        return ArtisanWorktableReflection.isContainer(container);
    }

    @Override
    @Nullable
    public MachineRecipeSurface openSurface(Container container) {
        ArtisanWorktableReflection.Binding binding = ArtisanWorktableReflection.bind(container);
        return binding == null ? null : new ArtisanWorktableSurface(binding);
    }
}
