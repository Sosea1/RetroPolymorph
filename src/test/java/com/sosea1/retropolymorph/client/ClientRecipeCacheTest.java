package com.sosea1.retropolymorph.client;

import com.sosea1.retropolymorph.api.RecipeOption;
import com.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientRecipeCacheTest {

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void replacingMatrixWithIdenticalIngredientsInvalidatesChoicesWithoutChangingPersistentKeys() {
        Item item = new Item().setRegistryName("test", "surface_input");
        SwitchingGrid context = new SwitchingGrid();
        context.matrix.setInventorySlotContents(0, new ItemStack(item));
        ClientRecipeCache cache = new ClientRecipeCache();
        assertTrue(cache.refreshInputs(context));
        String before = com.sosea1.retropolymorph.preference.InputFingerprint.create(context);
        cache.setChoices(Collections.singletonList(new RecipeOption("test:first", new ItemStack(item))), null);
        context.matrix = new net.minecraft.inventory.InventoryCrafting(context.owner, 3, 3);
        context.matrix.setInventorySlotContents(0, new ItemStack(item));
        assertTrue(cache.refreshInputs(context));
        assertTrue(cache.getChoices().isEmpty());
        assertEquals(before, com.sosea1.retropolymorph.preference.InputFingerprint.create(context));
    }

    private static final class SwitchingGrid implements com.sosea1.retropolymorph.api.RecipeSelectionContext {
        final Container owner = new Container() {
            @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return false; }
        };
        net.minecraft.inventory.InventoryCrafting matrix = new net.minecraft.inventory.InventoryCrafting(owner, 3, 3);
        @Override public Container getContainer() { return owner; }
        @Override public Slot getResultSlot() { return null; }
        @Override public net.minecraft.inventory.InventoryCrafting getRecipeMatrix() { return matrix; }
        @Override public List<net.minecraft.item.crafting.IRecipe> findAllMatches(World world) { return Collections.emptyList(); }
        @Override public String getRecipeKey(net.minecraft.item.crafting.IRecipe recipe) { return null; }
        @Override public boolean select(String key, World world) { return false; }
        @Override public void clearSelection() { }
        @Override public String getSelectedRecipeKey() { return null; }
    }

    @Test
    void replacingTemplateIngredientInvalidatesDisplayedChoicesButConsumingOneDoesNot() {
        Item first = new Item();
        Item second = new Item();
        MutableContext context = new MutableContext(new ItemStack(first, 3));
        ClientRecipeCache cache = new ClientRecipeCache();

        assertTrue(cache.refreshInputs(context));
        cache.setChoices(Arrays.asList(
                new RecipeOption("test:first", new ItemStack(first)),
                new RecipeOption("test:second", new ItemStack(second))), null);

        context.input = new ItemStack(first, 2);
        assertTrue(cache.refreshInputs(context));
        assertEquals(2, cache.getChoices().size());

        context.input = new ItemStack(second);
        assertTrue(cache.refreshInputs(context));
        assertTrue(cache.getChoices().isEmpty());
    }

    @Test
    void readsSurfaceTokenOnceWhenCapturingChangedInputs() {
        MutableContext context = new MutableContext(new ItemStack(new Item(), 3));
        ClientRecipeCache cache = new ClientRecipeCache();
        cache.refreshInputs(context);
        context.tokenReads = 0;
        context.input.setCount(2);

        assertTrue(cache.refreshInputs(context));
        assertEquals(1, context.tokenReads);
    }

    private static final class MutableContext implements SelectionContext {
        private ItemStack input;
        private int tokenReads;

        private MutableContext(ItemStack input) {
            this.input = input;
        }

        @Override public Container getContainer() { return null; }
        @Override public Slot getResultSlot() { return null; }
        @Override public int getInputCount() { return 1; }
        @Override public int getClientStateToken() { tokenReads++; return 0; }
        @Override public ItemStack getInputStack(int index) { return this.input; }
        @Override public List<RecipeOption> findOptions(World world) { return Collections.emptyList(); }
        @Override public boolean select(String recipeKey, World world) { return false; }
        @Override public void clearSelection() { }
        @Override @Nullable public String getSelectedRecipeKey() { return null; }
    }
}
