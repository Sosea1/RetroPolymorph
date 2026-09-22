package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class InputPreferenceKeysTest {

    private static Item firstItem;
    private static Item secondItem;

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        firstItem = new Item().setRegistryName("test", "first");
        secondItem = new Item().setRegistryName("test", "second");
    }

    @Test
    public void knownShapelessRecipePersistsAnUnorderedAlias() {
        GridContext context = new GridContext();
        context.set(0, new ItemStack(firstItem));
        context.set(8, new ItemStack(secondItem));

        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.fromStacks(new ItemStack(firstItem)));
        ingredients.add(Ingredient.fromStacks(new ItemStack(secondItem)));
        IRecipe recipe = new ShapelessRecipes("test", new ItemStack(firstItem), ingredients);

        List<String> keys = InputPreferenceKeys.storageKeys(context, recipe, null);
        assertEquals(2, keys.size());
        assertTrue(keys.contains(InputFingerprint.create(context)));
        assertTrue(keys.contains(InputFingerprint.createUnordered(context)));
    }

    @Test
    public void lookupPrefersSpecificPositionalAliasBeforeUnorderedAlias() {
        GridContext context = new GridContext();
        context.set(0, new ItemStack(firstItem));
        context.set(8, new ItemStack(secondItem));

        List<String> keys = InputPreferenceKeys.lookupKeys(context);
        assertEquals(InputFingerprint.create(context), keys.get(0));
        assertEquals(InputFingerprint.createUnordered(context), keys.get(1));
    }

    private static final class GridContext implements RecipeSelectionContext {
        private final Container owner = new NoopContainer();
        private final InventoryCrafting matrix = new InventoryCrafting(this.owner, 3, 3);

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
