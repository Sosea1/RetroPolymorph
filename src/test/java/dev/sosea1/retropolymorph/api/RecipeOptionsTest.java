package dev.sosea1.retropolymorph.api;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RecipeOptionsTest {

    private static Item testItem;
    private static Item secondTestItem;

    @BeforeAll
    public static void setup() {
        Bootstrap.register();
        testItem = new Item();
        secondTestItem = new Item();
    }

    @Test
    public void nullOrEmptyListReturnsEmpty() {
        assertTrue(RecipeOptions.sanitizeAndLimit(null, null).isEmpty());
        assertTrue(RecipeOptions.sanitizeAndLimit(Collections.<RecipeOption>emptyList(), null).isEmpty());
    }

    @Test
    public void singleOptionReturnsEmptySinceNoConflict() {
        List<RecipeOption> list = Collections.singletonList(
                new RecipeOption("mod:first", new ItemStack(testItem)));
        assertTrue(RecipeOptions.sanitizeAndLimit(list, null).isEmpty());
    }

    @Test
    public void filtersNullsDuplicatesAndEmptyOutputs() {
        List<RecipeOption> list = new ArrayList<RecipeOption>();
        list.add(new RecipeOption("mod:valid1", new ItemStack(testItem)));
        list.add(null);
        list.add(new RecipeOption("mod:valid1", new ItemStack(testItem))); // duplicate key
        list.add(new RecipeOption("mod:empty", ItemStack.EMPTY)); // empty output
        list.add(new RecipeOption("mod:valid2", new ItemStack(secondTestItem)));

        List<RecipeOption> result = RecipeOptions.sanitizeAndLimit(list, null);
        assertEquals(2, result.size());
        assertEquals("mod:valid1", result.get(0).getRecipeKey());
        assertEquals("mod:valid2", result.get(1).getRecipeKey());
    }

    @Test
    public void capsAtMaxServerOptionsAndPreservesSelectedOutsideWindow() {
        List<RecipeOption> list = new ArrayList<RecipeOption>();
        for (int i = 0; i < 20; i++) {
            list.add(new RecipeOption("mod:recipe_" + i, new ItemStack(testItem, i + 1)));
        }

        // Without selected outside window: strictly first 15
        List<RecipeOption> result = RecipeOptions.sanitizeAndLimit(list, null);
        assertEquals(RecipeOptions.MAX_SERVER_OPTIONS, result.size());
        assertEquals("mod:recipe_0", result.get(0).getRecipeKey());
        assertEquals("mod:recipe_14", result.get(14).getRecipeKey());

        // With selected at index 18: guaranteed to be placed into the list
        List<RecipeOption> resultWithSelected = RecipeOptions.sanitizeAndLimit(list, "mod:recipe_18");
        assertEquals(RecipeOptions.MAX_SERVER_OPTIONS, resultWithSelected.size());
        boolean foundSelected = false;
        for (RecipeOption opt : resultWithSelected) {
            if ("mod:recipe_18".equals(opt.getRecipeKey())) {
                foundSelected = true;
                break;
            }
        }
        assertTrue(foundSelected, "Selected recipe outside initial 15 window must be preserved");
    }
    @Test
    public void recipesWithIdenticalOutputsRemainDistinctByRecipeKey() {
        List<RecipeOption> list = new ArrayList<RecipeOption>();
        list.add(new RecipeOption("firstmod:same", new ItemStack(testItem, 2)));
        list.add(new RecipeOption("secondmod:same", new ItemStack(testItem, 2)));
        list.add(new RecipeOption("thirdmod:different", new ItemStack(secondTestItem)));

        List<RecipeOption> result = RecipeOptions.sanitizeAndLimit(list, null);
        assertEquals(3, result.size());
        assertEquals("firstmod:same", result.get(0).getRecipeKey());
        assertEquals("secondmod:same", result.get(1).getRecipeKey());
        assertEquals("thirdmod:different", result.get(2).getRecipeKey());
    }

    @Test
    public void nullOutputIsNormalizedAndFiltered() {
        List<RecipeOption> list = new ArrayList<RecipeOption>();
        list.add(new RecipeOption("mod:null_output", null));
        list.add(new RecipeOption("mod:valid1", new ItemStack(testItem)));
        list.add(new RecipeOption("mod:valid2", new ItemStack(secondTestItem)));

        List<RecipeOption> result = RecipeOptions.sanitizeAndLimit(list, null);
        assertEquals(2, result.size());
        assertEquals("mod:valid1", result.get(0).getRecipeKey());
        assertEquals("mod:valid2", result.get(1).getRecipeKey());
    }
}
