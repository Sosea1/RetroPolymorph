package com.sosea1.retropolymorph.compat.gregtech;

import com.sosea1.retropolymorph.core.CraftingMatrixExtension;
import com.sosea1.retropolymorph.core.SelectionServiceResult;
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
    private static boolean failSecondOutput;

    @org.junit.jupiter.api.AfterEach void resetRecipeFailure() { failSecondOutput = false; }

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
                new ItemStack(output, count, outputMeta)) {
            @Override public ItemStack getCraftingResult(net.minecraft.inventory.InventoryCrafting matrix) {
                if ("gt_second".equals(name) && failSecondOutput) {
                    throw new IllegalStateException("Broken recipe output");
                }
                return super.getCraftingResult(matrix);
            }
        };
        recipe.setRegistryName("retropolymorph_test", name);
        return recipe;
    }

    @Test
    void originalCeMissingOutputInventoryRejectsSelection() {
        CeFixture fixture = new CeFixture();
        fixture.resolver.result = null;
        assertFalse(fixture.context.select(second.getRegistryName().toString(), world));
        assertNull(fixture.resolver.cachedRecipe);
        assertNull(fixture.context.getSelectedRecipeKey());
    }

    @Test
    void originalCeBrokenRecipeOutputLeavesPreviousNativeCacheUntouched() {
        CeFixture fixture = new CeFixture();
        assertTrue(fixture.context.select(first.getRegistryName().toString(), world));
        failSecondOutput = true;
        assertFalse(fixture.context.select(second.getRegistryName().toString(), world));
        assertSame(first, fixture.resolver.cachedRecipe);
        assertEquals(first.getRegistryName().toString(), fixture.context.getSelectedRecipeKey());
        assertEquals(101, fixture.result.getStackInSlot(0).getMetadata());
    }

    private static final class CeFixture {
        final InventoryCraftResult result = new InventoryCraftResult();
        final GregTechWorkbenchReflectionTest.LegacyUI ui;
        final gregtech.common.metatileentities.storage.CraftingRecipeResolver resolver;
        final GregTechWorkbenchContext context;
        CeFixture() {
            Fixture template = new Fixture();
            template.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
            resolver = new gregtech.common.metatileentities.storage.CraftingRecipeResolver(
                    new net.minecraft.inventory.InventoryCrafting(
                            new GregTechWorkbenchReflectionTest.FakeDummyContainer(), 3, 3), result);
            resolver.craftingGrid.setStackInSlot(0, template.grid().getStackInSlot(0).copy());
            ui = new GregTechWorkbenchReflectionTest.LegacyUI(new gregtech.common.gui.widget.CraftingSlotWidget(resolver));
            Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
            context = new GregTechWorkbenchContext(container, GregTechWorkbenchReflection.bind(container));
        }
    }

    @Test
    void missingNativeCacheRejectsSelectionWithoutRememberingIt() {
        GregTechWorkbenchReflectionTest.LegacyLogic logic = new GregTechWorkbenchReflectionTest.LegacyLogic();
        logic.world = world;
        logic.craftingGrid.setStackInSlot(0, new ItemStack(input, 1, 1));
        GregTechWorkbenchContext context = nativeContext(logic);
        logic.cachedRecipeData = null;
        NBTTagCompound playerData = new NBTTagCompound();
        SelectionServiceResult result = com.sosea1.retropolymorph.core.SelectionService.handle(
                world, playerData, context, com.sosea1.retropolymorph.core.SelectionCommand.select(second.getRegistryName().toString()));
        assertFalse(result.isAccepted());
        assertNull(context.getSelectedRecipeKey());
        assertNull(com.sosea1.retropolymorph.preference.PlayerRecipePreferences.lookup(playerData,
                com.sosea1.retropolymorph.preference.ConflictFingerprint.create(result.getOptions())));
    }

    @Test
    void throwingNativeSetterRestoresPreviousChoiceAndDoesNotPublishSuccess() {
        GregTechWorkbenchReflectionTest.LegacyLogic logic = new GregTechWorkbenchReflectionTest.LegacyLogic();
        logic.world = world;
        logic.craftingGrid.setStackInSlot(0, new ItemStack(input, 1, 1));
        GregTechWorkbenchContext context = nativeContext(logic);
        assertTrue(context.select(first.getRegistryName().toString(), world));
        logic.cachedRecipeData.rejectedRecipe = second;
        NBTTagCompound data = new NBTTagCompound();
        SelectionServiceResult result = com.sosea1.retropolymorph.core.SelectionService.handle(
                world, data, context, com.sosea1.retropolymorph.core.SelectionCommand.select(second.getRegistryName().toString()));
        assertFalse(result.isAccepted());
        assertEquals(first.getRegistryName().toString(), context.getSelectedRecipeKey());
        assertSame(first, logic.cachedRecipe);
        assertSame(first, logic.cachedRecipeData.getRecipe());
        assertEquals(101, logic.result.getStackInSlot(0).getMetadata());
        assertNull(com.sosea1.retropolymorph.preference.PlayerRecipePreferences.lookup(data,
                com.sosea1.retropolymorph.preference.ConflictFingerprint.create(result.getOptions())));
    }

    private static GregTechWorkbenchContext nativeContext(GregTechWorkbenchReflectionTest.LegacyLogic logic) {
        GregTechWorkbenchReflectionTest.LegacyUI ui = new GregTechWorkbenchReflectionTest.LegacyUI(
                new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(logic, new Slot(logic.result, 0, 79, 35)));
        Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        return new GregTechWorkbenchContext(container, GregTechWorkbenchReflection.bind(container));
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
    void automaticReplyRestoresNativeRecipeAndEmptyOutputOncePerInputChange() {
        NativeAutoLogic logic = new NativeAutoLogic();
        logic.world = world;
        logic.craftingGrid.setStackInSlot(0, new ItemStack(input, 1, 1));
        logic.cachedRecipeData.setRecipe(first);
        GregTechWorkbenchContext context = nativeContext(logic);
        context.applyRemoteSelection(null);
        assertTrue(context.reconcileRemoteSelection(null, world));
        assertSame(first, logic.cachedRecipeData.getRecipe());
        assertEquals(101, logic.result.getStackInSlot(0).getMetadata());
        assertEquals(1, logic.updates);
        assertTrue(context.reconcileRemoteSelection(null, world));
        assertEquals(1, logic.updates, "Unchanged inputs must not reset native caches every frame");
        logic.craftingGrid.setStackInSlot(0, ItemStack.EMPTY);
        logic.craftingGrid.setStackInSlot(4, new ItemStack(input, 1, 1));
        assertTrue(context.reconcileRemoteSelection(null, world));
        assertEquals(2, logic.updates);
        assertEquals(101, logic.result.getStackInSlot(0).getMetadata());
    }

    @Test
    void automaticServerQueryRestoresNativeCacheBeforeReturningChoices() {
        NativeAutoLogic logic = new NativeAutoLogic();
        logic.world = world;
        logic.craftingGrid.setStackInSlot(4, new ItemStack(input, 1, 1));
        GregTechWorkbenchContext context = nativeContext(logic);
        assertEquals(3, context.findOptions(world).size());
        assertSame(first, logic.cachedRecipeData.getRecipe());
        assertEquals(101, logic.result.getStackInSlot(0).getMetadata());
        assertEquals(1, logic.updates);
    }

    private static final class NativeAutoLogic extends GregTechWorkbenchReflectionTest.LegacyLogic {
        int updates;
        public void updateCurrentRecipe() {
            updates++;
            net.minecraft.item.crafting.IRecipe cached = cachedRecipeData.getRecipe();
            // Preserve GT's native early return for a still-matching recipe.
            if (cached != null && cached.matches(inventoryCrafting, world)) return;
            net.minecraft.item.crafting.IRecipe resolved = first.matches(inventoryCrafting, world) ? first : null;
            cachedRecipeData.setRecipe(resolved);
            result.setInventorySlotContents(0, resolved == null ? ItemStack.EMPTY
                    : resolved.getCraftingResult(inventoryCrafting));
        }
        public void detectAndSendChanges(boolean init) { }
    }

    @Test
    void autoReplyDoesNotInventOutputWhileNativeEngineHasNoResult() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        assertTrue(fixture.context.reconcileRemoteSelection(second.getRegistryName().toString(), world));
        fixture.context.applyRemoteSelection(null);
        fixture.result.setInventorySlotContents(0, ItemStack.EMPTY);
        assertTrue(fixture.context.reconcileRemoteSelection(null, world));
        assertTrue(fixture.result.getStackInSlot(0).isEmpty(),
                "An automatic reply must not fabricate a locally craftable default result");
    }

    @Test
    void automaticRefreshKeepsNativeEngineResultInsteadOfReplacingIt() {
        GregTechWorkbenchReflectionTest.LegacyLogic logic = new GregTechWorkbenchReflectionTest.LegacyLogic();
        logic.world = world;
        logic.craftingGrid.setStackInSlot(0, new ItemStack(input, 1, 1));
        GregTechWorkbenchContext context = nativeContext(logic);
        logic.result.setInventorySlotContents(0, new ItemStack(output, 3, 103));
        context.refreshOutput();
        assertTrue(logic.updateRecipeCalled);
        assertEquals(103, logic.result.getStackInSlot(0).getMetadata(),
                "The adapter must use the result produced by native updateCurrentRecipe");
    }

    @Test
    void autoReplyClearsLocalSelectionButKeepsServerOutput() {
        Fixture fixture = new Fixture();
        fixture.grid().setStackInSlot(0, new ItemStack(input, 1, 1));
        fixture.context.reconcileRemoteSelection(second.getRegistryName().toString(), world);
        fixture.result.setInventorySlotContents(0, first.getRecipeOutput().copy());
        fixture.context.applyRemoteSelection(null);
        assertTrue(fixture.context.reconcileRemoteSelection(null, world));
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
        assertTrue(com.sosea1.retropolymorph.core.RecipeProbe.matches(logic.cachedRecipe, logic.inventoryCrafting, world));
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
