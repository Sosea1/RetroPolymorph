package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RecipeProbeTest {

    @BeforeEach
    public void setUp() {
        RecipeProbe.resetDiagnostics();
    }

    @Test
    public void malformedUndersizedRemainderListIsNormalized() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        assertEquals(9, matrix.getSizeInventory());

        IRecipe undersizedRecipe = new DummyRecipe(NonNullList.<ItemStack>create()); // size 0
        NonNullList<ItemStack> remainders = RecipeProbe.remainingItems(undersizedRecipe, matrix);

        assertEquals(9, remainders.size());
        for (int i = 0; i < 9; i++) {
            assertTrue(remainders.get(i).isEmpty());
        }
        assertEquals(1, RecipeProbe.getFailureCount());
    }

    @Test
    public void malformedOversizedRemainderListIsNormalized() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        assertEquals(9, matrix.getSizeInventory());

        NonNullList<ItemStack> oversizedList = NonNullList.withSize(15, ItemStack.EMPTY);
        IRecipe oversizedRecipe = new DummyRecipe(oversizedList); // size 15
        NonNullList<ItemStack> remainders = RecipeProbe.remainingItems(oversizedRecipe, matrix);

        assertEquals(9, remainders.size());
        for (int i = 0; i < 9; i++) {
            assertTrue(remainders.get(i).isEmpty());
        }
        assertEquals(1, RecipeProbe.getFailureCount());
    }

    @Test
    public void matchingSizeRemainderListIsPreserved() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        NonNullList<ItemStack> validList = NonNullList.withSize(9, ItemStack.EMPTY);
        IRecipe validRecipe = new DummyRecipe(validList);

        NonNullList<ItemStack> remainders = RecipeProbe.remainingItems(validRecipe, matrix);
        assertEquals(9, remainders.size());
        assertEquals(0, RecipeProbe.getFailureCount());
    }

    @Test
    public void oversizedMatrixRemaindersPreserveOriginalInventoryLayout() {
        InventoryCrafting matrix = new OversizedMatrix(new DummyContainer(), 3, 3);
        NonNullList<ItemStack> validList = NonNullList.withSize(9, ItemStack.EMPTY);
        IRecipe validRecipe = new DummyRecipe(validList);

        NonNullList<ItemStack> remainders = RecipeProbe.remainingItems(validRecipe, matrix);

        assertEquals(10, remainders.size());
        assertTrue(remainders.get(9).isEmpty());
        assertEquals(0, RecipeProbe.getFailureCount());
    }

    private static final class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return true;
        }
    }

    private static final class OversizedMatrix extends InventoryCrafting {
        private final int customSize;

        private OversizedMatrix(Container eventHandler, int width, int height) {
            super(eventHandler, width, height);
            this.customSize = width * height + 1;
        }

        @Override
        public int getSizeInventory() {
            return this.customSize;
        }
    }

    private static final class DummyRecipe implements IRecipe {
        private final NonNullList<ItemStack> remainders;

        public DummyRecipe(NonNullList<ItemStack> remainders) {
            this.remainders = remainders;
        }

        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            return true;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canFit(int width, int height) {
            return true;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return ItemStack.EMPTY;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            return this.remainders;
        }

        @Override
        public IRecipe setRegistryName(ResourceLocation name) {
            return this;
        }

        @Override
        public ResourceLocation getRegistryName() {
            return null;
        }

        @Override
        public Class<IRecipe> getRegistryType() {
            return IRecipe.class;
        }
    }
}
