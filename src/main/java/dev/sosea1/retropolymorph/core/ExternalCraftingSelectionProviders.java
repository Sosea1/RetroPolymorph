package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of active external crafting selection providers.
 */
public final class ExternalCraftingSelectionProviders {

    private static final List<ExternalCraftingSelectionProvider> PROVIDERS =
            new CopyOnWriteArrayList<ExternalCraftingSelectionProvider>();

    private ExternalCraftingSelectionProviders() {
    }

    public static void register(ExternalCraftingSelectionProvider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
        }
    }

    public static void unregister(ExternalCraftingSelectionProvider provider) {
        PROVIDERS.remove(provider);
    }

    static void resetForTests() {
        PROVIDERS.clear();
    }

    @Nullable
    public static ResourceLocation getSelectedRecipeId(InventoryCrafting matrix, @Nullable Container owner) {
        return getSelectedRecipeId(matrix, owner, null);
    }

    @Nullable
    public static ResourceLocation getSelectedRecipeId(
            InventoryCrafting matrix,
            @Nullable Container owner,
            @Nullable RecipeSelectionState currentState) {
        // 1. Authoritative providers: evaluated first in registration order, can override existing matrix state
        for (ExternalCraftingSelectionProvider provider : PROVIDERS) {
            if (provider.getPrecedence() == ExternalCraftingSelectionProvider.Precedence.AUTHORITATIVE) {
                ResourceLocation id = provider.getSelectedRecipeId(matrix, owner);
                if (id != null) {
                    return id;
                }
            }
        }

        // 2. Existing matrix selection: takes precedence over fallback providers
        if (currentState != null && currentState.getSelectedRecipeId() != null) {
            return null;
        }

        // 3. Fallback providers: evaluated in registration order only when no selection exists
        for (ExternalCraftingSelectionProvider provider : PROVIDERS) {
            if (provider.getPrecedence() == ExternalCraftingSelectionProvider.Precedence.FALLBACK_WHEN_EMPTY) {
                ResourceLocation id = provider.getSelectedRecipeId(matrix, owner);
                if (id != null) {
                    return id;
                }
            }
        }

        return null;
    }

    public static void notifyOutputResolved(InventoryCrafting matrix, @Nullable Container owner, IRecipe recipe) {
        for (ExternalCraftingSelectionProvider provider : PROVIDERS) {
            provider.onRecipeOutputResolved(matrix, owner, recipe);
        }
    }

    public static boolean shouldClearStateOnEmpty(InventoryCrafting matrix, @Nullable Container owner) {
        for (ExternalCraftingSelectionProvider provider : PROVIDERS) {
            if (provider.shouldClearStateOnEmpty(matrix, owner)) {
                return true;
            }
        }
        return false;
    }
}
