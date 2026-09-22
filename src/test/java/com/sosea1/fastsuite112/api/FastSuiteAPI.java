package com.sosea1.fastsuite112.api;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;

import java.util.Collections;

/** Test-only representation of Retro FastSuite's released candidate API. */
public final class FastSuiteAPI {
    private FastSuiteAPI() {
    }

    public static Iterable<IRecipe> getCandidateRecipes(InventoryCrafting inventory) {
        return Collections.emptyList();
    }
}
