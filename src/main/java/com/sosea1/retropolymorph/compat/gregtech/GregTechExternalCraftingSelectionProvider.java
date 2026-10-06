package com.sosea1.retropolymorph.compat.gregtech;

import com.sosea1.retropolymorph.core.ExternalCraftingSelectionProvider;
import com.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public final class GregTechExternalCraftingSelectionProvider implements ExternalCraftingSelectionProvider {

    public static final GregTechExternalCraftingSelectionProvider INSTANCE =
            new GregTechExternalCraftingSelectionProvider();

    private static final String GT_DUMMY_CONTAINER = "gregtech.api.util.DummyContainer";

    private GregTechExternalCraftingSelectionProvider() {
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.AUTHORITATIVE;
    }

    @Nullable
    @Override
    public ResourceLocation getSelectedRecipeId(
            InventoryCrafting matrix,
            @Nullable Container owner) {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getSelectedRecipeId(
            InventoryCrafting matrix,
            @Nullable Container owner,
            @Nullable RecipeSelectionState currentState) {
        if (isGregTechOwner(owner)) {
            return currentState != null ? currentState.getSelectedRecipeId() : null;
        }
        return null;
    }

    @Override
    public boolean shouldClearStateOnEmpty(InventoryCrafting matrix, @Nullable Container owner) {
        return isGregTechOwner(owner);
    }

    private static boolean isGregTechOwner(@Nullable Container owner) {
        if (owner == null) {
            return false;
        }
        String className = owner.getClass().getName();
        return GT_DUMMY_CONTAINER.equals(className) || className.contains("DummyContainer");
    }
}
