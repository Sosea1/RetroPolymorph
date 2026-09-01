package dev.sosea1.retropolymorph.compat.extendedcrafting;

import javax.annotation.Nullable;

/**
 * Captures Extended Crafting's singleton recipe manager when its optional mixin
 * is applied. Written once during class initialization and read thereafter.
 */
public final class ExtendedCraftingBridgeRegistry {

    @Nullable
    private static volatile ExtendedTableRecipeManagerBridge recipeManager;

    private ExtendedCraftingBridgeRegistry() {
    }

    public static void setRecipeManager(ExtendedTableRecipeManagerBridge manager) {
        recipeManager = manager;
    }

    @Nullable
    public static ExtendedTableRecipeManagerBridge getRecipeManager() {
        return recipeManager;
    }
}
