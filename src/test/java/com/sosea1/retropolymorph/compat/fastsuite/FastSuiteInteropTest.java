package com.sosea1.retropolymorph.compat.fastsuite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class FastSuiteInteropTest {

    @AfterEach
    public void resetCandidates() {
        com.sosea1.fastsuite112.api.FastSuiteAPI.candidates = null;
        com.sosea1.retropolymorph.core.RecipeProbe.resetDiagnostics();
    }

    @Test
    public void candidatesAreValidatedAndBrokenRecipesDoNotHideLaterMatches() throws Exception {
        Bootstrap.register();
        IRecipe matching = candidate(true, false);
        IRecipe nonmatching = candidate(false, false);
        IRecipe broken = candidate(false, true);
        IRecipe later = candidate(true, false);
        com.sosea1.fastsuite112.api.FastSuiteAPI.candidates = Arrays.asList(nonmatching, matching, broken, later);
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        World world = (World) ((sun.misc.Unsafe) field.get(null)).allocateInstance(TestWorld.class);
        InventoryCrafting matrix = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return false; }
        }, 3, 3);
        assertEquals(Arrays.asList(matching, later), FastSuiteInterop.findAllMatches(matrix, world));
    }

    private static IRecipe candidate(final boolean matches, final boolean throwsOnMatch) {
        return new ShapelessRecipes("", new ItemStack(new Item()), NonNullList.create()) {
            @Override public boolean matches(InventoryCrafting matrix, World world) {
                if (throwsOnMatch) { throw new IllegalStateException("Broken third-party recipe"); }
                return matches;
            }
        };
    }

    private static final class TestWorld extends World {
        TestWorld() { super(null, null, null, null, false); }
        @Override protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
    }

    @Test
    public void currentCandidateApiIsDetectedWithoutCheckingAModId() {
        assertTrue(FastSuiteInterop.isInstalled());
    }
}
