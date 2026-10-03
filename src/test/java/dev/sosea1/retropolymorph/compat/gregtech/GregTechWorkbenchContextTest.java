package dev.sosea1.retropolymorph.compat.gregtech;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.SelectionServiceResult;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class GregTechWorkbenchContextTest {
    private static Item input;
    private static Item output;
    private static ShapedRecipes first;
    private static ShapedRecipes second;
    private static ShapedRecipes alternative;
    private static World world;

    @BeforeAll
    static void setup() throws Exception {
        Bootstrap.register();
        input = new Item().setRegistryName("retropolymorph_test", "gt_input");
        output = new Item().setRegistryName("retropolymorph_test", "gt_output");
        ForgeRegistries.ITEMS.register(input);
        ForgeRegistries.ITEMS.register(output);
        first = recipe("gt_first", 1, 101, 1);
        second = recipe("gt_second", 1, 102, 4);
        alternative = new ShapedRecipes("", 1, 1,
                NonNullList.withSize(1, Ingredient.fromStacks(new ItemStack(input, 1, 1))),
                second.getRecipeOutput().copy()) {
            @Override public NonNullList<ItemStack> getRemainingItems(net.minecraft.inventory.InventoryCrafting matrix) {
                NonNullList<ItemStack> result = NonNullList.withSize(matrix.getSizeInventory(), ItemStack.EMPTY);
                result.set(0, new ItemStack(input, 1, 77));
                return result;
            }
        };
        alternative.setRegistryName("retropolymorph_test", "gt_alternative");
        ForgeRegistry<net.minecraft.item.crafting.IRecipe> registry =
                (ForgeRegistry<net.minecraft.item.crafting.IRecipe>) ForgeRegistries.RECIPES;
        registry.unfreeze();
        try { registry.register(first); registry.register(second); registry.register(alternative); }
        finally { registry.freeze(); }
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        world = (World) ((sun.misc.Unsafe) field.get(null)).allocateInstance(TestWorld.class);
    }

    private static ShapedRecipes recipe(String name, int inputMeta, int outputMeta, int count) {
        ShapedRecipes recipe = new ShapedRecipes("", 1, 1,
                NonNullList.withSize(1, Ingredient.fromStacks(new ItemStack(input, 1, inputMeta))),
                new ItemStack(output, count, outputMeta));
        recipe.setRegistryName("retropolymorph_test", name);
        return recipe;
    }

    @Test
    void authoritativeChoiceRepairsOutputOnlyUpdates() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        fixture.result.setInventorySlotContents(0, first.getRecipeOutput().copy());
        assertTrue(fixture.context.reconcileRemoteSelection(second.getRegistryName().toString(), world));
        assertEquals(102, fixture.result.getStackInSlot(0).getMetadata());
        fixture.result.setInventorySlotContents(0, first.getRecipeOutput().copy());
        assertTrue(fixture.context.reconcileRemoteSelection(second.getRegistryName().toString(), world));
        assertEquals(102, fixture.result.getStackInSlot(0).getMetadata());
    }

    @Test
    void incompleteOrDifferentGridDefersChoiceWithoutErasingNativeResult() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 2));
        fixture.result.setInventorySlotContents(0, new ItemStack(output, 2, 103));
        assertFalse(fixture.context.reconcileRemoteSelection(second.getRegistryName().toString(), world));
        assertEquals(103, fixture.result.getStackInSlot(0).getMetadata());
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        assertTrue(fixture.context.reconcileRemoteSelection(second.getRegistryName().toString(), world));
        assertEquals(102, fixture.result.getStackInSlot(0).getMetadata());
    }

    @Test
    void autoReplyClearsLocalSelectionButKeepsServerOutput() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        fixture.context.reconcileRemoteSelection(second.getRegistryName().toString(), world);
        fixture.result.setInventorySlotContents(0, first.getRecipeOutput().copy());
        fixture.context.applyRemoteSelection(null);
        assertNull(fixture.context.getSelectedRecipeKey());
        assertEquals(101, fixture.result.getStackInSlot(0).getMetadata());
    }

    @Test
    void outputRefreshDoesNotApplyStaleRecipeToChangedTemplate() {
        GregTechWorkbenchReflectionTest.LegacyLogic logic = new GregTechWorkbenchReflectionTest.LegacyLogic();
        logic.world = world;
        logic.craftingGrid.setStackInSlot(0, new ItemStack(input, 1, 1));
        GregTechWorkbenchReflectionTest.LegacyUI ui = new GregTechWorkbenchReflectionTest.LegacyUI(
                new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(logic, new Slot(logic.result, 0, 79, 35)));
        Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        GregTechWorkbenchContext context = new GregTechWorkbenchContext(container, GregTechWorkbenchReflection.bind(container));
        ((CraftingMatrixExtension) context.getRecipeMatrix()).retropolymorph$getOrCreateRecipeSelectionState()
                .select(second.getRegistryName());
        logic.craftingGrid.setStackInSlot(0, new ItemStack(input, 1, 2));
        context.refreshOutput();
        assertNull(logic.cachedRecipe, "Invalid choice must be dropped before native recipe resolution");
        assertNull(context.getSelectedRecipeKey());
    }

    @Test
    void selectingNewRecipeResetsOldIngredientSubstitutionsBeforeCrafting() {
        GregTechWorkbenchReflectionTest.LegacyLogic logic = new GregTechWorkbenchReflectionTest.LegacyLogic();
        logic.world = world;
        logic.craftingGrid.setStackInSlot(0, new ItemStack(input, 1, 1));
        logic.inventoryCrafting.setInventorySlotContents(0, new ItemStack(input, 1, 2));
        GregTechWorkbenchReflectionTest.LegacyUI ui = new GregTechWorkbenchReflectionTest.LegacyUI(
                new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(logic, new Slot(logic.result, 0, 79, 35)));
        Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        GregTechWorkbenchContext context = new GregTechWorkbenchContext(container, GregTechWorkbenchReflection.bind(container));
        assertTrue(context.select(second.getRegistryName().toString(), world));
        assertTrue(dev.sosea1.retropolymorph.core.RecipeProbe.matches(logic.cachedRecipe, logic.inventoryCrafting, world));
    }

    @Test
    void nativeMemoryOutputWinsOverPreviousChoiceForTheSameIngredients() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        assertTrue(fixture.context.select(first.getRegistryName().toString(), world));
        assertEquals(3, fixture.context.findOptions(world).size());
        SelectionServiceResult restored = GregTechWorkbenchMemory.restore(fixture.context, world,
                new NBTTagCompound(), second.getRecipeOutput().copy());
        assertNotNull(restored);
        assertTrue(restored.isAccepted());
        assertEquals(second.getRegistryName().toString(), restored.getSelectedRecipeKey());
        assertEquals(102, fixture.result.getStackInSlot(0).getMetadata());
        assertEquals(4, fixture.result.getStackInSlot(0).getCount());
    }

    @Test
    void unavailableNativeMemoryOutputDoesNotInstallAnUnrelatedRecipe() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        assertTrue(fixture.context.select(first.getRegistryName().toString(), world));
        assertNull(GregTechWorkbenchMemory.restore(fixture.context, world, new NBTTagCompound(),
                new ItemStack(output, 4, 999)));
        assertEquals(first.getRegistryName().toString(), fixture.context.getSelectedRecipeKey());
        assertEquals(101, fixture.result.getStackInSlot(0).getMetadata());
    }

    @Test
    void nativeMemoryPreservesCurrentRecipeWhenItsOutputAlreadyMatches() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        assertTrue(fixture.context.select(alternative.getRegistryName().toString(), world));
        SelectionServiceResult restored = GregTechWorkbenchMemory.restore(fixture.context, world,
                new NBTTagCompound(), second.getRecipeOutput().copy());
        assertNotNull(restored);
        assertEquals(alternative.getRegistryName().toString(), restored.getSelectedRecipeKey());
    }

    private static final class Fixture {
        final InventoryCraftResult result = new InventoryCraftResult();
        final GregTechWorkbenchReflectionTest.LegacyUI ui = new GregTechWorkbenchReflectionTest.LegacyUI(
                new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(null, new Slot(result, 0, 79, 35)));
        final Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        final GregTechWorkbenchContext context = new GregTechWorkbenchContext(container, GregTechWorkbenchReflection.bind(container));
        net.minecraftforge.items.ItemStackHandler grid() { return ui.holder.workbench.craftingGrid; }
    }

    private static final class TestWorld extends World {
        TestWorld() { super(null, null, null, null, false); }
        @Override public long getTotalWorldTime() { return 0L; }
        @Override protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
    }
}
