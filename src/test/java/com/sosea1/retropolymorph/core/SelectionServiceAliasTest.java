package com.sosea1.retropolymorph.core;

import com.sosea1.retropolymorph.api.RecipeSelectionContext;
import com.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import com.sosea1.retropolymorph.preference.ConflictFingerprint;
import com.sosea1.retropolymorph.preference.InputFingerprint;
import com.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SelectionServiceAliasTest {
    private static Item firstInput;
    private static Item secondInput;
    private static IRecipe selected;
    private static IRecipe alternative;
    @BeforeAll static void setup() {
        Bootstrap.register();
        firstInput = new Item().setRegistryName("retropolymorph_test", "alias_first");
        secondInput = new Item().setRegistryName("retropolymorph_test", "alias_second");
        selected = recipe("alias_selected", 1);
        alternative = recipe("alias_alternative", 2);
        ForgeRegistry<IRecipe> registry = (ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES;
        registry.unfreeze();
        try { registry.register(selected); registry.register(alternative); }
        finally { registry.freeze(); }
    }
    private static IRecipe recipe(String id, int outputCount) {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.add(Ingredient.fromStacks(new ItemStack(firstInput)));
        inputs.add(Ingredient.fromStacks(new ItemStack(secondInput)));
        return new ShapelessRecipes("", new ItemStack(firstInput, outputCount), inputs)
                .setRegistryName("retropolymorph_test", id);
    }

    @Test void clearRemovesShapelessAliasAfterCanonicalEviction() {
        GridContext context = new GridContext();
        context.matrix.setInventorySlotContents(0, new ItemStack(firstInput));
        context.matrix.setInventorySlotContents(1, new ItemStack(secondInput));
        NBTTagCompound data = new NBTTagCompound();
        String id = selected.getRegistryName().toString();
        assertTrue(SelectionService.handle(null, data, context, SelectionCommand.select(id)).isAccepted());
        String canonical = ConflictFingerprint.create(context.findOptions(null));
        assertNotNull(canonical);
        // The two bounded indexes are independent: canonical can disappear first.
        PlayerRecipePreferences.forget(data, canonical);
        String unordered = InputFingerprint.createUnordered(context);
        assertEquals(id, PlayerRecipePreferences.lookupInput(data, unordered));
        assertTrue(SelectionService.handle(null, data, context, SelectionCommand.clear()).isAccepted());
        assertNull(PlayerRecipePreferences.lookupInput(data, unordered));
        context.matrix.setInventorySlotContents(1, ItemStack.EMPTY);
        context.matrix.setInventorySlotContents(8, new ItemStack(secondInput));
        assertNull(PlayerRecipePreferences.lookupInput(data, InputFingerprint.createUnordered(context)));
    }

    private static final class GridContext implements RecipeSelectionContext {
        private final Container container = new Container() {
            @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return false; }
        };
        final InventoryCrafting matrix = new InventoryCrafting(container, 3, 3);
        private String chosen;
        @Override public Container getContainer() { return container; }
        @Override public Slot getResultSlot() { return null; }
        @Override public InventoryCrafting getRecipeMatrix() { return matrix; }
        @Override public List<IRecipe> findAllMatches(World world) { return Arrays.asList(selected, alternative); }
        @Override public String getRecipeKey(IRecipe recipe) { return recipe.getRegistryName().toString(); }
        @Override public boolean select(String key, World world) { chosen = key; return true; }
        @Override public void clearSelection() { chosen = null; }
        @Override public String getSelectedRecipeKey() { return chosen; }
        @Override public SelectionPersistencePolicy getPersistencePolicy() { return SelectionPersistencePolicy.PLAYER_PERSISTENT; }
    }
}
