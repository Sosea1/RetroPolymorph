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

    private static ItemStack[] emptyGrid() {
        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        return grid;
    }
}
