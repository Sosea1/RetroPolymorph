package dev.sosea1.retropolymorph.compat.rsb;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapters;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdaptersTestAccess;
import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProviders;
import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProvidersTestAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class RsbExternalCraftingSelectionProviderTest {

    @BeforeEach
    @AfterEach
    public void resetRegistries() {
        RecipeSelectionAdaptersTestAccess.reset();
        ExternalCraftingSelectionProvidersTestAccess.reset();
    }

    @Test
    public void readsSelectedRecipeFromRsbMatrixDelegate() {
        ItemStackHandler handler = new ItemStackHandler(9);
        ResourceLocation selected = new ResourceLocation("test", "chosen_recipe");
        RsbSlotAccess.storeSelection(handler, selected);

        InventoryCrafting matrix = new DelegateCraftingMatrix(handler);

        assertEquals(
                selected,
                RsbExternalCraftingSelectionProvider.INSTANCE.getSelectedRecipeId(matrix, null));
    }

    @Test
    public void integrationRegistersRsbSelectionProvider() {
        RsbIntegration.INSTANCE.registerEnabled();

        ItemStackHandler handler = new ItemStackHandler(9);
        ResourceLocation selected = new ResourceLocation("test", "registered_provider_recipe");
        RsbSlotAccess.storeSelection(handler, selected);
        InventoryCrafting matrix = new DelegateCraftingMatrix(handler);

        assertEquals(
                selected,
                ExternalCraftingSelectionProviders.getSelectedRecipeId(matrix, null, null));
    }

    @Test
    public void returnsNullAfterStoredSelectionIsCleared() {
        ItemStackHandler handler = new ItemStackHandler(9);
        RsbSlotAccess.storeSelection(handler, new ResourceLocation("test", "chosen_recipe"));
        RsbSlotAccess.storeSelection(handler, null);

        InventoryCrafting matrix = new DelegateCraftingMatrix(handler);

        assertNull(RsbExternalCraftingSelectionProvider.INSTANCE.getSelectedRecipeId(matrix, null));
    }

    private static final class DelegateCraftingMatrix extends InventoryCrafting {
        private final ItemStackHandler delegate;

        private DelegateCraftingMatrix(ItemStackHandler delegate) {
            super(new NoopContainer(), 3, 3);
            this.delegate = delegate;
        }

        public ItemStackHandler getDelegate() {
            return this.delegate;
        }
    }

    private static final class NoopContainer extends Container {
        @Override
        public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) {
        }

        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    }
}
