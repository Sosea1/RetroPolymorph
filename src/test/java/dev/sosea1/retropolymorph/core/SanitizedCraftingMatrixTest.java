package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SanitizedCraftingMatrixTest {

    @Test
    public void standardMatrixRemainsUnwrapped() {
        InventoryCrafting standard = new InventoryCrafting(new DummyContainer(), 3, 3);
        assertEquals(9, standard.getSizeInventory());
        InventoryCrafting sanitized = RecipeProbe.sanitizeMatrix(standard);
        assertSame(standard, sanitized);
    }

    @Test
    public void oversizedMatrixIsSanitizedTo2dDimensions() {
        OversizedModularMatrix oversized = new OversizedModularMatrix(new DummyContainer(), 3, 3);
        assertEquals(10, oversized.getSizeInventory());

        InventoryCrafting sanitized = RecipeProbe.sanitizeMatrix(oversized);
        assertTrue(sanitized instanceof SanitizedCraftingMatrix);
        assertEquals(9, sanitized.getSizeInventory());
        assertEquals(3, sanitized.getWidth());
        assertEquals(3, sanitized.getHeight());
        assertEquals(ItemStack.EMPTY, sanitized.getStackInSlot(9));
    }

    private static final class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return true;
        }
    }

    private static final class OversizedModularMatrix extends InventoryCrafting {
        private final int customSize;

        public OversizedModularMatrix(Container eventHandler, int width, int height) {
            super(eventHandler, width, height);
            this.customSize = width * height + 1; // 10 slots
        }

        @Override
        public int getSizeInventory() {
            return this.customSize;
        }
    }
}
