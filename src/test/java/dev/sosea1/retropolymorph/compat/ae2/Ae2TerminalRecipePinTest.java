package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.preference.InputFingerprint;
import dev.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class Ae2TerminalRecipePinTest {

    private static Item testItem;
    private static TestRecipe recipeA;
    private static TestRecipe recipeB;

    private static final ResourceLocation ID_RECIPE_A = new ResourceLocation("retropolymorph_test", "ae2_test_recipe_a");
    private static final ResourceLocation ID_RECIPE_B = new ResourceLocation("retropolymorph_test", "ae2_test_recipe_b");

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        testItem = new Item();
        testItem.setRegistryName(new ResourceLocation("retropolymorph_test", "test_item"));
        ForgeRegistries.ITEMS.register(testItem);

        recipeA = new TestRecipe(ID_RECIPE_A, new ItemStack(testItem, 1, 100));
        recipeB = new TestRecipe(ID_RECIPE_B, new ItemStack(testItem, 2, 200));

        // Unlock registry for test dummy registrations
        ForgeRegistry<IRecipe> registry = (ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES;
        registry.unfreeze();
        try {
            registry.register(recipeA);
            registry.register(recipeB);
        } finally {
            registry.freeze();
        }

        dev.sosea1.retropolymorph.core.CraftingPreferenceSeeder.reset();
        Ae2Integration.INSTANCE.registerEnabled();
    }

    @BeforeEach
    public void setUp() {
        dev.sosea1.retropolymorph.core.CraftingPreferenceSeeder.reset();
        Ae2SelectionStore.clearAll();
        RecipeProbe.resetDiagnostics();
        recipeA.setShouldMatch(true);
        recipeB.setShouldMatch(true);
    }

    @AfterEach
    public void tearDown() {
        Ae2MatrixChangeScope.exit();
    }

    @Test
    public void testPinResultSuccessWhenMatches() {
        MockAe2Container container = new MockAe2Container();
        // Put test item in slot 0
        container.getMatrixSlot(0).putStack(new ItemStack(testItem, 1, 0));

        Ae2SelectionStore.set(container, ID_RECIPE_A);
        container.retropolymorph$setAe2SelectedRecipeId(ID_RECIPE_A);

        boolean pinned = Ae2TerminalRecipePin.handleMatrixChangedReturn(container, "test");
        assertTrue(pinned);

        ItemStack result = container.getResultSlot().getStack();
        assertFalse(result.isEmpty());
        assertEquals(1, result.getCount());
        assertEquals(100, result.getMetadata());
        assertEquals(recipeA, container.retropolymorph$getAe2CurrentRecipe());
    }

    @Test
    public void testGhostResultClearedWhenRecipeBroken() {
        MockAe2Container container = new MockAe2Container();
        // Simulate broken input (e.g. removed one cobblestone)
        container.getMatrixSlot(0).putStack(new ItemStack(testItem, 1, 0));
        // Result slot initially has ghost output
        container.getResultSlot().putStack(new ItemStack(testItem, 4, 999));

        DummyWorld world = createDummyWorld();
        NBTTagCompound playerData = new NBTTagCompound();
        EntityPlayerMP player = createMockPlayer(world, playerData);
        container.setPlayer(player);

        // Recipe no longer matches the matrix
        recipeA.setShouldMatch(false);
        Ae2SelectionStore.set(container, ID_RECIPE_A);
        container.retropolymorph$setAe2SelectedRecipeId(ID_RECIPE_A);
        container.retropolymorph$setAe2CurrentRecipe(recipeA);

        boolean pinned = Ae2TerminalRecipePin.handleMatrixChangedReturn(container, "test");
        assertFalse(pinned);

        // Ghost result MUST be cleared!
        assertTrue(container.getResultSlot().getStack().isEmpty(), "Ghost output was not cleared on broken recipe!");
        assertNull(container.retropolymorph$getAe2SelectedRecipeId());
        assertNull(container.retropolymorph$getAe2CurrentRecipe());
    }

    @Test
    public void testGhostResultClearedWhenMatrixEmpty() {
        MockAe2Container container = new MockAe2Container();
        // Matrix is empty
        container.getResultSlot().putStack(new ItemStack(testItem, 4, 999));

        Ae2SelectionStore.set(container, ID_RECIPE_A);
        container.retropolymorph$setAe2SelectedRecipeId(ID_RECIPE_A);

        boolean pinned = Ae2TerminalRecipePin.handleMatrixChangedReturn(container, "test");
        assertFalse(pinned);

        assertTrue(container.getResultSlot().getStack().isEmpty());
        assertNull(container.retropolymorph$getAe2SelectedRecipeId());
        assertNull(container.retropolymorph$getAe2CurrentRecipe());
    }

    @Test
    public void testPreseedAtHeadRestoresPreferenceWithoutDelay() {
        MockAe2Container container = new MockAe2Container();
        container.getMatrixSlot(0).putStack(new ItemStack(testItem, 1, 0));

        DummyWorld world = createDummyWorld();
        NBTTagCompound playerData = new NBTTagCompound();
        EntityPlayerMP player = createMockPlayer(world, playerData);
        container.setPlayer(player);

        Ae2CraftingTermAdapter adapter = Ae2CraftingTermAdapter.INSTANCE;
        dev.sosea1.retropolymorph.api.RecipeSelectionContext context =
                (dev.sosea1.retropolymorph.api.RecipeSelectionContext) adapter.probe(container).getContext();
        assertNotNull(context);

        String fingerprint = InputFingerprint.create(context);
        assertNotNull(fingerprint);
        PlayerRecipePreferences.rememberInput(playerData, fingerprint, ID_RECIPE_B.toString());

        // At HEAD: container has no active selection yet
        Ae2TerminalRecipePin.handleMatrixChangedHead(container);

        // Preference must be immediately preseeded into currentRecipe and selection
        assertEquals(recipeB, container.retropolymorph$getAe2CurrentRecipe(),
                "Preference should be seeded into currentRecipe immediately at HEAD!");
        assertEquals(ID_RECIPE_B, container.retropolymorph$getAe2SelectedRecipeId());
    }

    @Test
    public void testMatrixChangeScopeBridgesActiveContainer() {
        MockAe2Container container = new MockAe2Container();
        Ae2SelectionStore.set(container, ID_RECIPE_B);
        container.retropolymorph$setAe2SelectedRecipeId(ID_RECIPE_B);

        InventoryCrafting scratch = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer playerIn) { return false; }
        }, 3, 3);

        // Outside scope: returns null
        assertNull(Ae2ExternalCraftingSelectionProvider.INSTANCE.getSelectedRecipeId(scratch, null));

        // Inside scope: returns active container's selection
        Ae2MatrixChangeScope.enter(container);
        try {
            assertEquals(ID_RECIPE_B,
                    Ae2ExternalCraftingSelectionProvider.INSTANCE.getSelectedRecipeId(scratch, null));
        } finally {
            Ae2MatrixChangeScope.exit();
        }

        // Outside scope again: returns null
        assertNull(Ae2ExternalCraftingSelectionProvider.INSTANCE.getSelectedRecipeId(scratch, null));
    }

    @Test
    public void testClearSelectionRestoresNativeResultWhenMatrixNotEmpty() {
        MockAe2Container container = new MockAe2Container();
        container.getMatrixSlot(0).putStack(new ItemStack(testItem, 1, 0));

        DummyWorld world = createDummyWorld();
        NBTTagCompound playerData = new NBTTagCompound();
        EntityPlayerMP player = createMockPlayer(world, playerData);
        container.setPlayer(player);

        Ae2CraftingTermAdapter adapter = Ae2CraftingTermAdapter.INSTANCE;
        dev.sosea1.retropolymorph.api.RecipeSelectionContext context =
                (dev.sosea1.retropolymorph.api.RecipeSelectionContext) adapter.probe(container).getContext();
        assertNotNull(context);

        // Select Recipe B explicitly first
        context.select(ID_RECIPE_B.toString(), world);
        assertEquals(ID_RECIPE_B, container.retropolymorph$getAe2SelectedRecipeId());
        assertEquals(2, container.getResultSlot().getStack().getCount());

        // Clear explicit selection (e.g. right-click to return to auto/native)
        context.clearSelection();

        // Selection should be cleared, but native recipe (Recipe A) output should be restored!
        assertNull(container.retropolymorph$getAe2SelectedRecipeId());
        assertFalse(container.getResultSlot().getStack().isEmpty(),
                "Result slot must not be EMPTY when ingredients match a native recipe!");
        assertEquals(1, container.getResultSlot().getStack().getCount(),
                "Native recipe output count must be restored!");
    }

    // --- Mock Classes & Helpers ---

    private static DummyWorld createDummyWorld() {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            return (DummyWorld) unsafe.allocateInstance(DummyWorld.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static EntityPlayerMP createMockPlayer(World world, NBTTagCompound nbt) {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            EntityPlayerMP player = (EntityPlayerMP) unsafe.allocateInstance(EntityPlayerMP.class);

            for (Field field : net.minecraft.entity.Entity.class.getDeclaredFields()) {
                if (World.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    field.set(player, world);
                } else if (NBTTagCompound.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    field.set(player, nbt);
                }
            }
            return player;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static InventoryPlayer createMockInventoryPlayer(EntityPlayer player) {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            InventoryPlayer inv = (InventoryPlayer) unsafe.allocateInstance(InventoryPlayer.class);
            Field playerField = InventoryPlayer.class.getField("player");
            playerField.setAccessible(true);
            playerField.set(inv, player);
            return inv;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class DummyWorld extends World {
        protected DummyWorld() {
            super(null, null, null, null, false);
        }
        @Override protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
    }

    private static final class TestRecipe implements IRecipe {
        private final ResourceLocation id;
        private final ItemStack output;
        private boolean shouldMatch = true;

        TestRecipe(ResourceLocation id, ItemStack output) {
            this.id = id;
            this.output = output;
        }

        void setShouldMatch(boolean match) {
            this.shouldMatch = match;
        }

        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            return this.shouldMatch;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            return this.output.copy();
        }

        @Override
        public boolean canFit(int width, int height) {
            return true;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return this.output.copy();
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        }

        @Override
        public IRecipe setRegistryName(ResourceLocation name) {
            return this;
        }

        @Override
        public ResourceLocation getRegistryName() {
            return this.id;
        }

        @Override
        public Class<IRecipe> getRegistryType() {
            return IRecipe.class;
        }
    }

    private static final class MockMatrixSlot extends Slot implements Ae2CraftingMatrixSlot {
        MockMatrixSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }
    }

    private static final class MockResultSlot extends Slot implements Ae2CraftingResultSlot {
        MockResultSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }
    }

    private static final class MockAe2Container extends Container implements Ae2CraftingTermExtension {
        private final IInventory matrixInv = new InventoryBasic("matrix", false, 9);
        private final IInventory resultInv = new InventoryBasic("result", false, 1);

        private final MockMatrixSlot[] matrixSlots = new MockMatrixSlot[9];
        private final MockResultSlot resultSlot;
        private Slot playerSlot;

        @Nullable
        private IRecipe currentRecipe;
        @Nullable
        private ResourceLocation selectedRecipeId;

        MockAe2Container() {
            // 9 matrix slots arranged in 3x3 grid (x: 0, 18, 36, y: 0, 18, 36)
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int index = r * 3 + c;
                    MockMatrixSlot slot = new MockMatrixSlot(this.matrixInv, index, c * 18, r * 18);
                    this.matrixSlots[index] = slot;
                    this.addSlotToContainer(slot);
                }
            }
            // 1 result slot
            this.resultSlot = new MockResultSlot(this.resultInv, 0, 100, 18);
            this.addSlotToContainer(this.resultSlot);
        }

        void setPlayer(EntityPlayer player) {
            InventoryPlayer invPlayer = createMockInventoryPlayer(player);
            this.playerSlot = new Slot(invPlayer, 0, 0, 100);
            this.addSlotToContainer(this.playerSlot);
        }

        MockMatrixSlot getMatrixSlot(int index) {
            return this.matrixSlots[index];
        }

        MockResultSlot getResultSlot() {
            return this.resultSlot;
        }

        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }

        @Override
        public void onCraftMatrixChanged(IInventory inventoryIn) {
            Ae2TerminalRecipePin.handleMatrixChangedHead(this);
            Ae2TerminalRecipePin.handleMatrixChangedReturn(this, "onCraftMatrixChanged");
        }

        @Nullable
        @Override
        public IRecipe retropolymorph$getAe2CurrentRecipe() {
            return this.currentRecipe;
        }

        @Override
        public void retropolymorph$setAe2CurrentRecipe(@Nullable IRecipe recipe) {
            this.currentRecipe = recipe;
        }

        @Nullable
        @Override
        public ResourceLocation retropolymorph$getAe2SelectedRecipeId() {
            ResourceLocation stored = Ae2SelectionStore.get(this);
            return stored != null ? stored : this.selectedRecipeId;
        }

        @Override
        public void retropolymorph$setAe2SelectedRecipeId(@Nullable ResourceLocation recipeId) {
            this.selectedRecipeId = recipeId;
            Ae2SelectionStore.set(this, recipeId);
        }
    }
}
