package dev.sosea1.retropolymorph.core;

import javax.annotation.Nullable;

/**
 * Added to every vanilla/modded {@code InventoryCrafting} instance by Mixin.
 *
 * State is intentionally lazy. Most crafting matrices never need Retro Polymorph,
 * and some mods create temporary matrices frequently. Keeping the field null
 * until a user actually selects an alternate recipe avoids useless allocation
 * and GC pressure on the vanilla/modded crafting path.
 */
public interface CraftingMatrixExtension {

    @Nullable
    RecipeSelectionState retropolymorph$peekRecipeSelectionState();

    RecipeSelectionState retropolymorph$getOrCreateRecipeSelectionState();
}
