package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
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

    private static final class MutableContext implements SelectionContext {
        private ItemStack input;

        private MutableContext(ItemStack input) {
            this.input = input;
        }

        @Override public Container getContainer() { return null; }
        @Override public Slot getResultSlot() { return null; }
        @Override public int getInputCount() { return 1; }
        @Override public ItemStack getInputStack(int index) { return this.input; }
        @Override public List<RecipeOption> findOptions(World world) { return Collections.emptyList(); }
        @Override public boolean select(String recipeKey, World world) { return false; }
        @Override public void clearSelection() { }
        @Override @Nullable public String getSelectedRecipeKey() { return null; }
    }
}
