package dev.sosea1.retropolymorph.compat.rsb;

import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProvider;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Supplies external selection state from Retro Sophisticated Backpacks crafting matrices.
 */
public final class RsbExternalCraftingSelectionProvider implements ExternalCraftingSelectionProvider {

    public static final RsbExternalCraftingSelectionProvider INSTANCE = new RsbExternalCraftingSelectionProvider();

    private RsbExternalCraftingSelectionProvider() {
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.FALLBACK_WHEN_EMPTY;
    }

    @Nullable
    @Override
    public ResourceLocation getSelectedRecipeId(InventoryCrafting matrix, @Nullable Container owner) {
        if (matrix == null) {
            return null;
        }
        Object delegate = RsbBindings.invokeNoArg(matrix, "getDelegate");
        if (delegate instanceof IItemHandler) {
            return RsbSlotAccess.getStoredSelection((IItemHandler) delegate);
        }
        return null;
    }
}
