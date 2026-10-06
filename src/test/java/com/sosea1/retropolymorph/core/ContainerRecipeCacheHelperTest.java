package com.sosea1.retropolymorph.core;

import com.sosea1.retropolymorph.compat.tconstruct.TinkersCraftingStationAccess;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerRecipeCacheHelperTest {

    static class DummyRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            return false;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canFit(int width, int height) {
            return false;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return ItemStack.EMPTY;
        }
    }

    static class FastBenchContainerMock extends Container {
        public IRecipe lastRecipe;
        protected IRecipe lastLastRecipe;

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return true;
        }
    }

    static class TinkersAccessContainerMock extends Container implements TinkersCraftingStationAccess {
        boolean cleared = false;

        @Override
        public void retropolymorph$clearLastRecipe() {
            this.cleared = true;
        }

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return true;
        }
    }

    @Test
    void testClearLastRecipeViaReflection() {
        FastBenchContainerMock container = new FastBenchContainerMock();
        container.lastRecipe = new DummyRecipe();
        container.lastLastRecipe = new DummyRecipe();

        ContainerRecipeCacheHelper.clearLastRecipe(container);

        assertNull(container.lastRecipe, "lastRecipe should be set to null");
        assertNull(container.lastLastRecipe, "lastLastRecipe should be set to null");
    }

    @Test
    void testClearLastRecipeViaInterface() {
        TinkersAccessContainerMock container = new TinkersAccessContainerMock();
        assertFalse(container.cleared);

        ContainerRecipeCacheHelper.clearLastRecipe(container);

        assertTrue(container.cleared, "retropolymorph$clearLastRecipe should have been called");
    }

    @Test
    void testClearLastRecipeHandlesNull() {
        ContainerRecipeCacheHelper.clearLastRecipe(null);
    }
}
