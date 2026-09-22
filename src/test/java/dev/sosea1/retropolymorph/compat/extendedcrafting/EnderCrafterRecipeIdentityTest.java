package dev.sosea1.retropolymorph.compat.extendedcrafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnderCrafterRecipeIdentityTest {

    private static final class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    }

    @BeforeAll
    static void setupAll() {
        Bootstrap.register();
    }

    private static class MockRecipe implements IRecipe {
        private final ResourceLocation id;
        private final ItemStack staticOutput;
        private final ItemStack dynamicOutput;
        private final boolean throwsOnStaticOutput;

        MockRecipe(String id, ItemStack staticOutput, ItemStack dynamicOutput) {
            this(id, staticOutput, dynamicOutput, false);
        }

        MockRecipe(String id, ItemStack staticOutput, ItemStack dynamicOutput, boolean throwsOnStaticOutput) {
            this.id = new ResourceLocation("test", id);
            this.staticOutput = staticOutput;
            this.dynamicOutput = dynamicOutput;
            this.throwsOnStaticOutput = throwsOnStaticOutput;
        }

        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            return true;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            return dynamicOutput != null ? dynamicOutput.copy() : ItemStack.EMPTY;
        }

        @Override
        public boolean canFit(int width, int height) {
            return true;
        }

        @Override
        public ItemStack getRecipeOutput() {
            if (throwsOnStaticOutput) {
                throw new UnsupportedOperationException("Dynamic recipe has no static output!");
            }
            return staticOutput != null ? staticOutput.copy() : ItemStack.EMPTY;
        }

        @Override
        public IRecipe setRegistryName(ResourceLocation name) {
            return this;
        }

        @Nullable
        @Override
        public ResourceLocation getRegistryName() {
            return id;
        }

        @Override
        public Class<IRecipe> getRegistryType() {
            return IRecipe.class;
        }
    }

    @Test
    @DisplayName("resolveEffectiveOutput prefers non-empty static output")
    void testStaticOutputResolution() {
        ItemStack staticStack = new ItemStack(net.minecraft.init.Items.DIAMOND, 2);
        ItemStack dynamicStack = new ItemStack(net.minecraft.init.Items.GOLD_INGOT, 1);
        MockRecipe recipe = new MockRecipe("static_recipe", staticStack, dynamicStack);

        ItemStack resolved = EnderCrafterRecipeIdentity.resolveEffectiveOutput(recipe, null);
        assertFalse(resolved.isEmpty());
        assertEquals(net.minecraft.init.Items.DIAMOND, resolved.getItem());
        assertEquals(2, resolved.getCount());
    }

    @Test
    @DisplayName("resolveEffectiveOutput falls back to dynamic result when static is empty")
    void testDynamicOutputResolution() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        ItemStack dynamicStack = new ItemStack(net.minecraft.init.Items.EMERALD, 3);
        MockRecipe recipe = new MockRecipe("dynamic_recipe", ItemStack.EMPTY, dynamicStack);

        ItemStack resolved = EnderCrafterRecipeIdentity.resolveEffectiveOutput(recipe, matrix);
        assertFalse(resolved.isEmpty());
        assertEquals(net.minecraft.init.Items.EMERALD, resolved.getItem());
        assertEquals(3, resolved.getCount());
    }

    @Test
    @DisplayName("resolveEffectiveOutput safely catches exceptions from getRecipeOutput and falls back to dynamic")
    void testThrowingStaticOutputResolution() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        ItemStack dynamicStack = new ItemStack(net.minecraft.init.Items.NETHER_STAR, 1);
        MockRecipe recipe = new MockRecipe("throwing_recipe", null, dynamicStack, true);

        ItemStack resolved = EnderCrafterRecipeIdentity.resolveEffectiveOutput(recipe, matrix);
        assertFalse(resolved.isEmpty());
        assertEquals(net.minecraft.init.Items.NETHER_STAR, resolved.getItem());
    }

    @Test
    @DisplayName("matchesExpectedOutput correctly validates static and dynamic outputs")
    void testMatchesExpectedOutput() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        ItemStack target = new ItemStack(net.minecraft.init.Items.EMERALD, 3);
        MockRecipe recipe = new MockRecipe("dynamic_recipe", ItemStack.EMPTY, target);

        assertTrue(EnderCrafterRecipeIdentity.matchesExpectedOutput(recipe, matrix, target));

        ItemStack different = new ItemStack(net.minecraft.init.Items.DIAMOND, 3);
        assertFalse(EnderCrafterRecipeIdentity.matchesExpectedOutput(recipe, matrix, different));

        assertFalse(EnderCrafterRecipeIdentity.matchesExpectedOutput(recipe, matrix, ItemStack.EMPTY));
        assertFalse(EnderCrafterRecipeIdentity.matchesExpectedOutput(recipe, matrix, null));
        assertFalse(EnderCrafterRecipeIdentity.matchesExpectedOutput(null, matrix, target));
    }

    @Test
    @DisplayName("Simulate recipe list shift: index points to wrong recipe but fallback recovers correct recipe by output")
    void testRecipeListShiftFallbackRecovery() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        ItemStack targetOutput = new ItemStack(net.minecraft.init.Items.DIAMOND, 1);
        ItemStack otherOutput = new ItemStack(net.minecraft.init.Items.IRON_INGOT, 1);

        MockRecipe recipeA = new MockRecipe("recipe_a", otherOutput, otherOutput);
        MockRecipe recipeB = new MockRecipe("recipe_b", ItemStack.EMPTY, targetOutput);

        List<MockRecipe> recipes = new ArrayList<MockRecipe>();
        recipes.add(recipeA);
        recipes.add(recipeB);

        int savedIndex = 0;
        ItemStack expectedOutput = targetOutput;

        assertFalse(EnderCrafterRecipeIdentity.matchesExpectedOutput(recipes.get(savedIndex), matrix, expectedOutput));

        MockRecipe recovered = null;
        int recoveredIndex = -1;
        for (int i = 0; i < recipes.size(); i++) {
            if (EnderCrafterRecipeIdentity.matchesExpectedOutput(recipes.get(i), matrix, expectedOutput)) {
                recovered = recipes.get(i);
                recoveredIndex = i;
                break;
            }
        }

        assertNotNull(recovered);
        assertEquals(1, recoveredIndex);
        assertEquals(recipeB, recovered);
    }

    @Test
    @DisplayName("Simulate recipe deletion: neither index nor search matches -> selection rejected")
    void testRecipeDeletionRejection() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        ItemStack targetOutput = new ItemStack(net.minecraft.init.Items.NETHER_STAR, 1);
        ItemStack otherOutput = new ItemStack(net.minecraft.init.Items.IRON_INGOT, 1);

        MockRecipe recipeA = new MockRecipe("recipe_a", otherOutput, otherOutput);
        List<MockRecipe> recipes = new ArrayList<MockRecipe>();
        recipes.add(recipeA);

        int savedIndex = 0;
        assertFalse(EnderCrafterRecipeIdentity.matchesExpectedOutput(recipes.get(savedIndex), matrix, targetOutput));

        boolean found = false;
        for (MockRecipe r : recipes) {
            if (EnderCrafterRecipeIdentity.matchesExpectedOutput(r, matrix, targetOutput)) {
                found = true;
                break;
            }
        }
        assertFalse(found);
    }
}
