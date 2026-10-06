package com.sosea1.retropolymorph.compat.rsb;

import com.cleanroommc.retrosophisticatedbackpacks.common.gui.BackpackContainer;
import com.sosea1.retropolymorph.core.CraftingMatrixExtension;
import com.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetroSophisticatedBackpackContextTest {
    private static Item upgrade;
    private static Item ingredient;
    @BeforeAll static void setup() {
        Bootstrap.register();
        upgrade = new Item().setRegistryName("retro_sophisticated_backpacks", "crafting_upgrade");
        ingredient = new Item();
    }

    @Test void removedUpgradeCannotExposeOrMutateItsFormerMatrix() {
        BackpackContainer container = new BackpackContainer();
        RsbCraftingAccess.ActiveCrafting active = install(container, 0);
        active.matrix.setInventorySlotContents(0, new ItemStack(ingredient));
        ((CraftingMatrixExtension) active.matrix).retropolymorph$getOrCreateRecipeSelectionState()
                .select(new ResourceLocation("test", "selected"));
        active.resultSlot.putStack(new ItemStack(ingredient, 3));
        RetroSophisticatedBackpackContext context = new RetroSophisticatedBackpackContext(container, active);
        assertEquals(9, context.getInputCount());
        container.outputRefreshes = 0;
        container.wrapper.upgrades.setStackInSlot(0, ItemStack.EMPTY);
        // RSB may remove the map entries after the item update; stale maps must not win.
        assertEquals(0, context.getInputCount());
        assertNull(context.getSelectedRecipeKey());
        assertNotSame(active.matrix, context.getRecipeMatrix());
        assertNull(context.getResultSlot());
        assertTrue(context.getInputStack(0).isEmpty());
        assertFalse(context.select("test:selected", null));
        context.clearSelection();
        context.applyRemoteSelection("test:other");
        assertEquals(new ResourceLocation("test", "selected"),
                ((CraftingMatrixExtension) active.matrix).retropolymorph$peekRecipeSelectionState().getSelectedRecipeId());
        assertEquals(3, active.resultSlot.getStack().getCount());
        assertEquals(0, container.outputRefreshes);
    }

    @Test void replacingUpgradeWithIdenticalInputsRebindsToTheNewSurface() {
        BackpackContainer container = new BackpackContainer();
        RsbCraftingAccess.ActiveCrafting first = install(container, 0);
        first.matrix.setInventorySlotContents(0, new ItemStack(ingredient));
        RetroSophisticatedBackpackContext context = new RetroSophisticatedBackpackContext(container, first);
        int oldToken = context.getClientStateToken();
        container.wrapper.upgrades.setStackInSlot(0, ItemStack.EMPTY);
        RsbCraftingAccess.ActiveCrafting second = install(container, 1);
        second.matrix.setInventorySlotContents(0, new ItemStack(ingredient));
        assertSame(second.matrix, context.getRecipeMatrix());
        assertNotEquals(oldToken, context.getClientStateToken());
    }

    @Test void sameIndexClientMirrorReplacementStopsReadingTheFormerHandler() {
        BackpackContainer container = new BackpackContainer();
        container.wrapper.upgrades.setStackInSlot(0, new ItemStack(upgrade));
        Matrix oldMirror = new Matrix(container);
        net.minecraftforge.items.ItemStackHandler oldHandler = new net.minecraftforge.items.ItemStackHandler(9);
        oldHandler.setStackInSlot(0, new ItemStack(ingredient));
        Slot oldResult = new com.cleanroommc.retrosophisticatedbackpacks.common.gui.IndexedModularCraftingSlot(oldMirror, 0);
        container.craftingSlotInstances.put(0, oldResult);
        RetroSophisticatedBackpackContext context = new RetroSophisticatedBackpackContext(container,
                new RsbCraftingAccess.ActiveCrafting(0, oldMirror, oldResult, true, oldHandler));
        Matrix replacement = new Matrix(container);
        replacement.setInventorySlotContents(0, new ItemStack(ingredient));
        Slot newResult = new com.cleanroommc.retrosophisticatedbackpacks.common.gui.IndexedModularCraftingSlot(replacement, 0);
        container.craftingSlotInstances.put(0, newResult);
        assertSame(replacement, context.getRecipeMatrix());
        assertSame(newResult, context.getResultSlot());
        context.clearSelection();
        assertFalse(oldMirror.retropolymorph$getOrCreateRecipeSelectionState().hasSelection());
        assertEquals(1, oldHandler.getStackInSlot(0).getCount());
    }

    @Test void unchangedNbtMirrorKeepsItsMatrixAcrossReadsButReplacementRebinds() {
        BackpackContainer container = new BackpackContainer();
        ItemStack stack = new ItemStack(upgrade);
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setTag("Matrix", new net.minecraftforge.items.ItemStackHandler(9).serializeNBT());
        stack.setTagCompound(tag);
        container.wrapper.upgrades.setStackInSlot(0, stack);
        container.craftingSlotInstances.put(0, new Slot(new InventoryCraftResult(), 0, 0, 0));
        RsbCraftingAccess.ActiveCrafting active = RsbCraftingAccess.resolveActiveCrafting(container);
        assertNotNull(active);
        RetroSophisticatedBackpackContext context = new RetroSophisticatedBackpackContext(container, active);
        InventoryCrafting first = context.getRecipeMatrix();
        assertSame(first, context.getRecipeMatrix());
        container.wrapper.upgrades.setStackInSlot(0, stack.copy());
        InventoryCrafting replacement = context.getRecipeMatrix();
        assertNotSame(first, replacement);
        assertSame(replacement, context.getRecipeMatrix());
    }

    @Test void unchangedClientMirrorDoesNotCopySourceNbtAgain() {
        BackpackContainer container = new BackpackContainer();
        Matrix mirror = new Matrix(container);
        net.minecraftforge.items.ItemStackHandler handler = new net.minecraftforge.items.ItemStackHandler(9);
        CountingTag tag = new CountingTag();
        tag.setString("value", "first");
        ItemStack source = new ItemStack(ingredient, 3);
        source.setTagCompound(tag);
        handler.setStackInSlot(0, source);
        RsbCraftingAccess.ActiveCrafting active = new RsbCraftingAccess.ActiveCrafting(
                0, mirror, null, true, handler);

        RsbCraftingAccess.refreshClientMatrix(active);
        assertNotSame(source, mirror.getStackInSlot(0));
        tag.copies = 0;
        RsbCraftingAccess.refreshClientMatrix(active);
        assertEquals(0, tag.copies, "unchanged mirror should not clone the source tag");

        tag.setString("value", "second");
        assertEquals("first", mirror.getStackInSlot(0).getTagCompound().getString("value"));
        source.setCount(2);
        RsbCraftingAccess.refreshClientMatrix(active);
        assertEquals(1, tag.copies);
        assertEquals(2, mirror.getStackInSlot(0).getCount());
        assertEquals("second", mirror.getStackInSlot(0).getTagCompound().getString("value"));
        assertNotSame(source, mirror.getStackInSlot(0));
        handler.setStackInSlot(0, ItemStack.EMPTY);
        RsbCraftingAccess.refreshClientMatrix(active);
        assertTrue(mirror.getStackInSlot(0).isEmpty());
    }

    private static final class CountingTag extends net.minecraft.nbt.NBTTagCompound {
        int copies;
        @Override public net.minecraft.nbt.NBTTagCompound copy() {
            copies++;
            return (net.minecraft.nbt.NBTTagCompound) super.copy();
        }
    }

    private static RsbCraftingAccess.ActiveCrafting install(BackpackContainer container, int index) {
        container.wrapper.upgrades.setStackInSlot(index, new ItemStack(upgrade));
        InventoryCrafting matrix = new Matrix(container);
        Slot result = new Slot(new InventoryCraftResult(), 0, 0, 0);
        container.inventoryCraftingInstances.put(index, matrix);
        container.craftingSlotInstances.put(index, result);
        return new RsbCraftingAccess.ActiveCrafting(index, matrix, result, false);
    }

    private static final class Matrix extends InventoryCrafting implements CraftingMatrixExtension {
        private RecipeSelectionState state;
        Matrix(BackpackContainer container) { super(container, 3, 3); }
        @Override public RecipeSelectionState retropolymorph$getOrCreateRecipeSelectionState() {
            if (state == null) { state = new RecipeSelectionState(); }
            return state;
        }
        @Override public RecipeSelectionState retropolymorph$peekRecipeSelectionState() { return state; }
        @Override public net.minecraft.inventory.Container retropolymorph$getCraftingOwner() { return null; }
    }
}
