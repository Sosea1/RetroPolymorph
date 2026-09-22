package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Provider hook allowing modular compatibility layers (such as AE2 or RSB)
 * to supply external recipe selection state to CraftingManagerMixin without
 * coupling core mixin bytecode to third-party mods.
 */
public interface ExternalCraftingSelectionProvider {

    /**
     * Precedence tier of this external crafting selection provider.
     */
    enum Precedence {
        /**
         * Authoritative provider (e.g. AE2 terminal/scratch execution scope).
         * Evaluated first; can override existing matrix state.
         */
        AUTHORITATIVE,

        /**
         * Fallback provider (e.g. RSB stored selection).
         * Evaluated only when no authoritative provider matches AND existing matrix state has no selection.
         */
        FALLBACK_WHEN_EMPTY
    }

    /**
     * Returns the precedence tier for this provider. Defaults to {@link Precedence#AUTHORITATIVE}.
     */
    default Precedence getPrecedence() {
        return Precedence.AUTHORITATIVE;
    }

    /**
     * Attempts to resolve an externally selected recipe ID for the given matrix and container owner.
     *
     * @param matrix the crafting inventory
     * @param owner the container owning the matrix, if known
     * @return the selected recipe ID, or null if this provider has no selection
     */
    @Nullable
    ResourceLocation getSelectedRecipeId(InventoryCrafting matrix, @Nullable Container owner);

    /**
     * Called when a recipe output is resolved for the matrix.
     */
    default void onRecipeOutputResolved(InventoryCrafting matrix, @Nullable Container owner, IRecipe recipe) {
    }

    /**
     * Whether an empty external selection should clear existing matrix state.
     */
    default boolean shouldClearStateOnEmpty(InventoryCrafting matrix, @Nullable Container owner) {
        return false;
    }
}
