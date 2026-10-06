package com.sosea1.fastsuite112.api;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;


/** Test-only representation of Retro FastSuite's released candidate API. */
public final class FastSuiteAPI {
    public static Iterable<IRecipe> candidates;
    private FastSuiteAPI() {
    }

    public static Iterable<IRecipe> getCandidateRecipes(InventoryCrafting inventory) {
        // A conservative candidate API may return the full registry. Returning
        // an empty list would hide every recipe from all resolver integration tests.
        return candidates != null ? candidates : net.minecraft.item.crafting.CraftingManager.REGISTRY;
    }
}
