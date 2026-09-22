package dev.sosea1.retropolymorph.api;

import net.minecraft.util.ResourceLocation;

/** Immutable metadata for a registered recipe-selection adapter. */
public final class RecipeSelectionAdapterInfo {

    private final ResourceLocation id;
    private final int priority;
    private final RecipeSelectionAdapter adapter;

    RecipeSelectionAdapterInfo(ResourceLocation id, int priority, RecipeSelectionAdapter adapter) {
        this.id = id;
        this.priority = priority;
        this.adapter = adapter;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public int getPriority() {
        return this.priority;
    }

    public RecipeSelectionAdapter getAdapter() {
        return this.adapter;
    }
}
