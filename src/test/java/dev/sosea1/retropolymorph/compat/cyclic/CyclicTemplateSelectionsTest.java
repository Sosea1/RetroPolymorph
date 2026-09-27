package dev.sosea1.retropolymorph.compat.cyclic;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class CyclicTemplateSelectionsTest {

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void oneDirtAndFullGridKeepIndependentChoicesAcrossSaveAndLoad() {
        CyclicTemplateSelections selections = new CyclicTemplateSelections();

        ItemStack[] oneDirt = emptyGrid();
        oneDirt[0] = new ItemStack(Blocks.DIRT, 1);
        ItemStack[] fullGrid = emptyGrid();
        Arrays.fill(fullGrid, new ItemStack(Blocks.DIRT, 1));
        ResourceLocation gold = new ResourceLocation("test", "gold");
        ResourceLocation iron = new ResourceLocation("test", "iron");

        selections.remember(oneDirt, gold);
        selections.remember(fullGrid, iron);
        assertEquals(gold, selections.lookup(oneDirt));
        assertEquals(iron, selections.lookup(fullGrid));

        NBTTagCompound saved = new NBTTagCompound();
        selections.writeToNBT(saved);
        CyclicTemplateSelections loaded = new CyclicTemplateSelections();
        loaded.readFromNBT(saved);
        assertEquals(gold, loaded.lookup(oneDirt));
        assertEquals(iron, loaded.lookup(fullGrid));

        loaded.forget(oneDirt);
        assertNull(loaded.lookup(oneDirt));
        assertEquals(iron, loaded.lookup(fullGrid));
    }

    @Test
    void sameSingleItemTemplateKeepsSelectionInEveryCraftingSlot() {
        CyclicTemplateSelections selections = new CyclicTemplateSelections();
        ResourceLocation emerald = new ResourceLocation("test", "emerald");

        ItemStack[] center = emptyGrid();
        center[4] = new ItemStack(Blocks.DIRT, 1);
        selections.remember(center, emerald);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack[] moved = emptyGrid();
            moved[slot] = new ItemStack(Blocks.DIRT, 1);
            assertEquals(emerald, selections.lookup(moved),
                    "absolute Cyclic grid slot must not create another remembered craft");
        }
    }

    @Test
    void translatedShapeSharesSelectionButDifferentShapeDoesNot() {
        CyclicTemplateSelections selections = new CyclicTemplateSelections();
        ResourceLocation selected = new ResourceLocation("test", "shape");

        ItemStack[] upperLeft = emptyGrid();
        upperLeft[0] = new ItemStack(Blocks.DIRT, 1);
        upperLeft[1] = new ItemStack(Blocks.STONE, 1);
        upperLeft[3] = new ItemStack(Blocks.COBBLESTONE, 1);
        selections.remember(upperLeft, selected);

        ItemStack[] lowerRight = emptyGrid();
        lowerRight[4] = new ItemStack(Blocks.DIRT, 1);
        lowerRight[5] = new ItemStack(Blocks.STONE, 1);
        lowerRight[7] = new ItemStack(Blocks.COBBLESTONE, 1);
        assertEquals(selected, selections.lookup(lowerRight));

        ItemStack[] differentShape = emptyGrid();
        differentShape[0] = new ItemStack(Blocks.DIRT, 1);
        differentShape[1] = new ItemStack(Blocks.STONE, 1);
        differentShape[4] = new ItemStack(Blocks.COBBLESTONE, 1);
        assertNull(selections.lookup(differentShape));
    }

    private static ItemStack[] emptyGrid() {
        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        return grid;
    }
}
