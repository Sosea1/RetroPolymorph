package dev.sosea1.retropolymorph.compat.gregtech;

import com.cleanroommc.modularui.screen.ModularContainer;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Collection;
import net.minecraftforge.items.ItemStackHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GregTechWorkbenchReflectionTest {

    private static Item testItem;

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        testItem = new Item().setRegistryName("test", "test_item");
    }

    public static class FakeModularContainer extends ModularContainer {
        final FakeModularSyncManager syncManager = new FakeModularSyncManager();

        public FakeModularContainer() {
            setSyncManager(this.syncManager);
        }
    }

    public static class FakeModularSyncManager {
        FakePanelSyncManager mainPSM = new FakePanelSyncManager();
        final Map<String, FakePanelSyncManager> panelSyncManagerMap = new HashMap<String, FakePanelSyncManager>();

        public FakePanelSyncManager getMainPSM() {
            return this.mainPSM;
        }

        public boolean isClient() {
            return false;
        }
    }

    public static class FakePanelSyncManager {
        final Map<String, Object> syncHandlers = new HashMap<String, Object>();
    }

    public static class FakeCachedRecipeData {
        IRecipe recipe;

        public void setRecipe(IRecipe recipe) {
            this.recipe = recipe;
        }

        public IRecipe getRecipe() {
            return this.recipe;
        }
    }

    public static class FakeCraftingRecipeLogic {
        final InventoryCrafting matrix = new InventoryCrafting(null, 3, 3);
        final IInventory result = new InventoryCraftResult();
        final FakeCachedRecipeData cachedRecipeData = new FakeCachedRecipeData();
        boolean updateRecipeCalled = false;
        boolean detectAndSendChangesCalled = false;

        public InventoryCrafting getCraftingMatrix() {
            return this.matrix;
        }

        public IInventory getCraftingResultInventory() {
            return this.result;
        }

        public void updateCurrentRecipe() {
            this.updateRecipeCalled = true;
        }

        public void detectAndSendChanges(boolean init) {
            this.detectAndSendChangesCalled = true;
        }
    }

    private static IRecipe createFakeRecipe(ItemStack output) {
        return new IRecipe() {
            @Override
            public boolean matches(InventoryCrafting inv, World worldIn) {
                return true;
            }

            @Override
            public ItemStack getCraftingResult(InventoryCrafting inv) {
                return output.copy();
            }

            @Override
            public boolean canFit(int width, int height) {
                return true;
            }

            @Override
            public ItemStack getRecipeOutput() {
                return output;
            }

            @Override
            public IRecipe setRegistryName(ResourceLocation name) {
                return this;
            }

            @Nullable
            @Override
            public ResourceLocation getRegistryName() {
                return new ResourceLocation("test", "fake_recipe");
            }

            @Override
            public Class<IRecipe> getRegistryType() {
                return IRecipe.class;
            }
        };
    }

    public static class LegacyLogic extends gregtech.common.metatileentities.storage.CraftingRecipeLogic {
        World world;
        final ItemStackHandler craftingGrid = new ItemStackHandler(9);
        final InventoryCrafting inventoryCrafting = new InventoryCrafting(new FakeDummyContainer(), 3, 3);
        final IInventory result = new InventoryCraftResult();
        final FakeCachedRecipeData cachedRecipeData = new FakeCachedRecipeData();
        IRecipe cachedRecipe;
        ItemStack oldResult = ItemStack.EMPTY;
        boolean updateRecipeCalled;

        public IInventory getCraftingResultInventory() {
            return this.result;
        }

        private void updateCurrentRecipe() {
            this.updateRecipeCalled = true;
        }
    }

    public static class LegacyWorkbench {
        final ItemStackHandler craftingGrid = new ItemStackHandler(9);

        public ItemStackHandler getCraftingGrid() {
            return this.craftingGrid;
        }
    }

    public static class LegacyHolder {
        final LegacyWorkbench workbench = new LegacyWorkbench();

        public LegacyWorkbench getMetaTileEntity() {
            return this.workbench;
        }
    }

    public static class LegacyUI {
        public final LegacyHolder holder = new LegacyHolder();
        final Object outputWidget;

        LegacyUI(Object outputWidget) {
            this.outputWidget = outputWidget;
        }

        public Collection<?> getFlatVisibleWidgetCollection() {
            return Collections.singletonList(this.outputWidget);
        }
    }

    @Test
    void bindsReleasedGtceuServerAndUpdatesBothExecutionRecipeCaches() {
        LegacyLogic logic = new LegacyLogic();
        Slot output = new Slot(logic.result, 0, 79, 35);
        LegacyUI ui = new LegacyUI(new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(logic, output));
        Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        container.inventorySlots.add(output);

        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNotNull(binding, "Released GTCEu uses its legacy GUI with CraftingRecipeLogic");
        assertSame(logic.inventoryCrafting, binding.executionMatrix);
        assertSame(output, binding.resultSlot);

        IRecipe selected = createFakeRecipe(new ItemStack(testItem, 4));
        GregTechWorkbenchReflection.refresh(binding, selected);
        assertSame(selected, logic.cachedRecipe);
        assertSame(selected, logic.cachedRecipeData.getRecipe());
        assertEquals(4, logic.result.getStackInSlot(0).getCount());
        assertEquals(4, logic.oldResult.getCount(), "Legacy tick must not discard selection as an output mismatch");

        GregTechWorkbenchReflection.refresh(binding, null);
        assertNull(logic.cachedRecipe);
        assertNull(logic.cachedRecipeData.getRecipe());
        assertTrue(logic.updateRecipeCalled);
    }

    @Test
    void bindsReleasedGtceuClientWithoutServerRecipeLogicAndKeepsLiveGrid() {
        IInventory result = new InventoryCraftResult();
        Slot output = new Slot(result, 0, 79, 35);
        LegacyUI ui = new LegacyUI(new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(null, output));
        ui.holder.workbench.craftingGrid.setStackInSlot(0, new ItemStack(testItem, 1));
        Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        container.inventorySlots.add(output);

        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNotNull(binding, "GTCEu deliberately omits server logic from its client widget");
        assertSame(output, binding.resultSlot);
        assertEquals(testItem, binding.matrix.getStackInSlot(0).getItem());
        assertEquals(testItem, binding.matrix.getStackInRowAndColumn(0, 0).getItem());
        assertFalse(binding.matrix.isEmpty());
        assertSame(binding.matrix, GregTechWorkbenchReflection.bind(container).matrix,
                "Repeated probes must preserve the matrix selection state");

        ui.holder.workbench.craftingGrid.setStackInSlot(0, ItemStack.EMPTY);
        assertTrue(binding.matrix.getStackInSlot(0).isEmpty());
        assertTrue(binding.matrix.isEmpty());
        GregTechWorkbenchReflection.refresh(binding, createFakeRecipe(new ItemStack(testItem, 3)));
        assertEquals(3, result.getStackInSlot(0).getCount());
    }

    @Test
    void clientAutoReplyMustNotEraseNativeOutput() {
        IInventory result = new InventoryCraftResult();
        result.setInventorySlotContents(0, new ItemStack(testItem, 4));
        Slot output = new Slot(result, 0, 79, 35);
        LegacyUI ui = new LegacyUI(new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(null, output));
        Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        GregTechWorkbenchReflection.refresh(GregTechWorkbenchReflection.bind(container), null);
        assertEquals(4, result.getStackInSlot(0).getCount(), "Auto is not an instruction to empty the output slot");
    }

    @Test
    void serverOptionsUseGhostTemplateInsteadOfMutatedExecutionGrid() {
        LegacyLogic logic = new LegacyLogic();
        logic.craftingGrid.setStackInSlot(0, new ItemStack(testItem, 1, 5));
        logic.inventoryCrafting.setInventorySlotContents(0, new ItemStack(testItem, 1, 9));
        Slot output = new Slot(logic.result, 0, 79, 35);
        LegacyUI ui = new LegacyUI(new gregtech.common.gui.widget.craftingstation.CraftingSlotWidget(logic, output));
        Container container = new gregtech.api.gui.impl.ModularUIContainer(ui);
        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNotNull(binding);
        assertEquals(5, binding.matrix.getStackInSlot(0).getMetadata());
        logic.inventoryCrafting.setInventorySlotContents(0, ItemStack.EMPTY);
        assertEquals(5, binding.matrix.getStackInSlot(0).getMetadata());
    }

    @Test
    void testBindCeuFindsLogicInMainPSMRegardlessOfPanelName() {
        FakeModularContainer container = new FakeModularContainer();
        FakeCraftingRecipeLogic logic = new FakeCraftingRecipeLogic();
        container.syncManager.mainPSM.syncHandlers.put("arbitrary_key:123", logic);

        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNotNull(binding, "Binding should be resolved from mainPSM");
        assertSame(container, binding.container);
        assertNotNull(binding.matrix);
        assertNotNull(binding.resultSlot);
        assertEquals(80, binding.resultSlot.xPos);
        assertEquals(47, binding.resultSlot.yPos);
    }

    @Test
    void testBindCeuFindsLogicInSubPanel() {
        FakeModularContainer container = new FakeModularContainer();
        container.syncManager.mainPSM = null; // empty main

        FakePanelSyncManager subPanel = new FakePanelSyncManager();
        FakeCraftingRecipeLogic logic = new FakeCraftingRecipeLogic();
        subPanel.syncHandlers.put("crafting_logic", logic);
        container.syncManager.panelSyncManagerMap.put("crafting_station", subPanel);

        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNotNull(binding, "Binding should be resolved from panelSyncManagerMap");
        assertSame(container, binding.container);
    }

    @Test
    void testBindReturnsNullForEmptySyncManager() {
        FakeModularContainer container = new FakeModularContainer();
        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNull(binding);
    }

    @Test
    void testRefreshCeuWithSelectedRecipeInjectsRecipeAndUpdatesSlot() {
        FakeModularContainer container = new FakeModularContainer();
        FakeCraftingRecipeLogic logic = new FakeCraftingRecipeLogic();
        container.syncManager.mainPSM.syncHandlers.put("crafting_logic", logic);

        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNotNull(binding);

        IRecipe fakeRecipe = createFakeRecipe(new ItemStack(testItem, 4));
        GregTechWorkbenchReflection.refresh(binding, fakeRecipe);

        assertSame(fakeRecipe, logic.cachedRecipeData.getRecipe());
        assertEquals(testItem, logic.result.getStackInSlot(0).getItem());
        assertEquals(4, logic.result.getStackInSlot(0).getCount());
        assertFalse(logic.updateRecipeCalled, "updateCurrentRecipe should not be called when recipe is explicitly injected");
        assertTrue(logic.detectAndSendChangesCalled, "detectAndSendChanges should be invoked on server side");
    }

    @Test
    void testRefreshCeuWithNullClearsRecipeAndCallsUpdateCurrentRecipe() {
        FakeModularContainer container = new FakeModularContainer();
        FakeCraftingRecipeLogic logic = new FakeCraftingRecipeLogic();
        logic.cachedRecipeData.setRecipe(createFakeRecipe(new ItemStack(testItem, 1)));
        container.syncManager.mainPSM.syncHandlers.put("crafting_logic", logic);

        GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(container);
        assertNotNull(binding);

        GregTechWorkbenchReflection.refresh(binding, null);

        assertNull(logic.cachedRecipeData.getRecipe());
        assertTrue(logic.updateRecipeCalled, "updateCurrentRecipe must be called when selection cleared");
        assertTrue(logic.detectAndSendChangesCalled, "detectAndSendChanges should be invoked on server side");
    }

    public static class FakeDummyContainer extends Container {
        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return true;
        }
    }

    @Test
    void testExternalCraftingSelectionProvider() {
        GregTechExternalCraftingSelectionProvider provider =
                GregTechExternalCraftingSelectionProvider.INSTANCE;

        FakeDummyContainer dummy = new FakeDummyContainer() {};
        // Class name matches endsWith(".DummyContainer") or GT_DUMMY_CONTAINER
        // Let's create an anonymous subclass of gregtech.api.util.DummyContainer via subclass or class name
        class DummyContainer extends Container {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
                return true;
            }
        }
        DummyContainer gtDummy = new DummyContainer();

        InventoryCrafting matrix = new InventoryCrafting(gtDummy, 3, 3);
        assertTrue(provider.shouldClearStateOnEmpty(matrix, gtDummy));
        assertFalse(provider.shouldClearStateOnEmpty(matrix, new Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
                return true;
            }
        }));

        RecipeSelectionState state = new RecipeSelectionState();
        ResourceLocation id = new ResourceLocation("minecraft", "iron_pickaxe");
        state.select(id);
        assertEquals(id, provider.getSelectedRecipeId(matrix, gtDummy, state));
        assertNull(provider.getSelectedRecipeId(matrix, null, state));
    }
}
