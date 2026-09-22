package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class InputFingerprintTest {

    private static Item item;
    private static Item secondItem;

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        item = new Item().setRegistryName("test", "input");
        secondItem = new Item().setRegistryName("test", "second_input");
    }

    @Test
    public void stackCountDoesNotChangeLogicalInputFingerprint() {
        StubContext one = new StubContext(new ItemStack(item, 1, 2), 0);
        StubContext sixtyFour = new StubContext(new ItemStack(item, 64, 2), 0);

        String first = InputFingerprint.create(one);
        String second = InputFingerprint.create(sixtyFour);

        assertNotNull(first);
        assertEquals(first, second);
    }

    @Test
    public void metadataNbtAndStateTokenParticipate() {
        ItemStack baseStack = new ItemStack(item, 1, 2);
        ItemStack differentMeta = new ItemStack(item, 1, 3);
        ItemStack differentNbt = new ItemStack(item, 1, 2);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("mode", "special");
        differentNbt.setTagCompound(tag);

        String base = InputFingerprint.create(new StubContext(baseStack, 0));
        assertNotEquals(base, InputFingerprint.create(new StubContext(differentMeta, 0)));
        assertNotEquals(base, InputFingerprint.create(new StubContext(differentNbt, 0)));
        assertNotEquals(base, InputFingerprint.create(new StubContext(baseStack.copy(), 1)));
    }

    @Test
    public void oneItemCraftingGridIgnoresAbsolutePosition() {
        GridContext topLeft = GridContext.singleAt(3, 3, 0, new ItemStack(item, 1, 0));
        GridContext center = GridContext.singleAt(3, 3, 4, new ItemStack(item, 1, 0));
        GridContext bottomRight = GridContext.singleAt(3, 3, 8, new ItemStack(item, 1, 0));

        assertEquals(InputFingerprint.create(topLeft), InputFingerprint.create(center));
        assertEquals(InputFingerprint.create(topLeft), InputFingerprint.create(bottomRight));
    }

    @Test
    public void translatedShapeKeepsFingerprintButDifferentShapeDoesNot() {
        GridContext upperPair = new GridContext(3, 3);
        upperPair.set(0, new ItemStack(item, 1, 0));
        upperPair.set(1, new ItemStack(secondItem, 1, 0));

        GridContext lowerPair = new GridContext(3, 3);
        lowerPair.set(6, new ItemStack(item, 1, 0));
        lowerPair.set(7, new ItemStack(secondItem, 1, 0));

        GridContext verticalPair = new GridContext(3, 3);
        verticalPair.set(0, new ItemStack(item, 1, 0));
        verticalPair.set(3, new ItemStack(secondItem, 1, 0));

        assertEquals(InputFingerprint.create(upperPair), InputFingerprint.create(lowerPair));
        assertNotEquals(InputFingerprint.create(upperPair), InputFingerprint.create(verticalPair));
    }


    @Test
    public void horizontalMirrorUsesTheOppositeRelativeShape() {
        GridContext mainDiagonal = new GridContext(3, 3);
        mainDiagonal.set(0, new ItemStack(item, 1, 0));
        mainDiagonal.set(4, new ItemStack(secondItem, 1, 0));

        GridContext antiDiagonal = new GridContext(3, 3);
        antiDiagonal.set(1, new ItemStack(item, 1, 0));
        antiDiagonal.set(3, new ItemStack(secondItem, 1, 0));

        assertNotEquals(InputFingerprint.create(mainDiagonal), InputFingerprint.create(antiDiagonal));
        assertEquals(InputFingerprint.createHorizontalMirror(mainDiagonal), InputFingerprint.create(antiDiagonal));
        assertEquals(InputFingerprint.createHorizontalMirror(antiDiagonal), InputFingerprint.create(mainDiagonal));
    }

    @Test
    public void unorderedFingerprintIgnoresSlotPermutationButKeepsStackIdentity() {
        GridContext first = new GridContext(3, 3);
        first.set(0, new ItemStack(item, 1, 0));
        first.set(8, new ItemStack(secondItem, 1, 0));

        GridContext permuted = new GridContext(3, 3);
        permuted.set(2, new ItemStack(secondItem, 32, 0));
        permuted.set(3, new ItemStack(item, 64, 0));

        assertNotEquals(InputFingerprint.create(first), InputFingerprint.create(permuted));
        assertEquals(InputFingerprint.createUnordered(first), InputFingerprint.createUnordered(permuted));

        GridContext differentMeta = new GridContext(3, 3);
        differentMeta.set(2, new ItemStack(secondItem, 1, 1));
        differentMeta.set(3, new ItemStack(item, 1, 0));
        assertNotEquals(InputFingerprint.createUnordered(first), InputFingerprint.createUnordered(differentMeta));
    }

    @Test
    public void originalGridDimensionsRemainPartOfFingerprint() {
        GridContext twoByTwo = GridContext.singleAt(2, 2, 0, new ItemStack(item, 1, 0));
        GridContext threeByThree = GridContext.singleAt(3, 3, 0, new ItemStack(item, 1, 0));

        assertNotEquals(InputFingerprint.create(twoByTwo), InputFingerprint.create(threeByThree));
    }

    private static final class StubContext implements SelectionContext {
        private final ItemStack input;
        private final int stateToken;

        private StubContext(ItemStack input, int stateToken) {
            this.input = input;
            this.stateToken = stateToken;
        }

        @Override public Container getContainer() { return null; }
        @Override public Slot getResultSlot() { return null; }
        @Override public int getInputCount() { return 1; }
        @Override public ItemStack getInputStack(int index) { return index == 0 ? this.input : ItemStack.EMPTY; }
        @Override public int getClientStateToken() { return this.stateToken; }
        @Override public List<RecipeOption> findOptions(World world) { return Collections.emptyList(); }
        @Override public boolean select(String recipeKey, World world) { return false; }
        @Override public void clearSelection() { }
        @Nullable @Override public String getSelectedRecipeKey() { return null; }
    }

    private static final class GridContext implements RecipeSelectionContext {
        private final Container owner = new NoopContainer();
        private final InventoryCrafting matrix;

        private GridContext(int width, int height) {
            this.matrix = new InventoryCrafting(this.owner, width, height);
        }

        private static GridContext singleAt(int width, int height, int slot, ItemStack stack) {
            GridContext context = new GridContext(width, height);
            context.set(slot, stack);
            return context;
        }

        private void set(int slot, ItemStack stack) {
            this.matrix.setInventorySlotContents(slot, stack);
        }

        @Override public Container getContainer() { return this.owner; }
        @Override public InventoryCrafting getRecipeMatrix() { return this.matrix; }
        @Override public Slot getResultSlot() { return null; }
        @Override public List<IRecipe> findAllMatches(World world) { return Collections.emptyList(); }
        @Nullable @Override public String getRecipeKey(IRecipe recipe) { return null; }
        @Override public boolean select(String recipeKey, World world) { return false; }
        @Override public void clearSelection() { }
        @Nullable @Override public String getSelectedRecipeKey() { return null; }
    }

    private static final class NoopContainer extends Container {
        @Override public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) { }
        @Override public boolean canInteractWith(EntityPlayer playerIn) { return false; }
    }
}
