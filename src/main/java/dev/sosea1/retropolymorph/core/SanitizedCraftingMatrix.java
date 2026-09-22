package dev.sosea1.retropolymorph.core;

import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * Clean 2D view of a crafting matrix that constrains {@link #getSizeInventory()}
 * to {@code width * height}.
 *
 * <p>ModularUI's {@code InventoryCraftingWrapper} sets {@code size = width * height + 1}
 * and stores the crafting result at index 9. Vanilla and modded shapeless recipes
 * loop up to {@code getSizeInventory()}, observing both the input items and the
 * previous craft result, causing {@code matches()} to fail whenever the output
 * slot is occupied.</p>
 */
public final class SanitizedCraftingMatrix extends InventoryCrafting implements CraftingMatrixExtension {

    private final InventoryCrafting delegate;
    private final int cleanSize;

    public SanitizedCraftingMatrix(InventoryCrafting delegate) {
        super(
                delegate instanceof CraftingMatrixExtension
                        ? ((CraftingMatrixExtension) delegate).retropolymorph$getCraftingOwner()
                        : null,
                delegate.getWidth(),
                delegate.getHeight());
        this.delegate = delegate;
        this.cleanSize = delegate.getWidth() * delegate.getHeight();
    }

    public InventoryCrafting getDelegate() {
        return this.delegate;
    }

    @Override
    public int getSizeInventory() {
        return this.cleanSize;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.cleanSize; i++) {
            if (!getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < this.cleanSize
                ? this.delegate.getStackInSlot(index)
                : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return index >= 0 && index < this.cleanSize
                ? this.delegate.decrStackSize(index, count)
                : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return index >= 0 && index < this.cleanSize
                ? this.delegate.removeStackFromSlot(index)
                : ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index >= 0 && index < this.cleanSize) {
            this.delegate.setInventorySlotContents(index, stack);
        }
    }

    @Override
    public ItemStack getStackInRowAndColumn(int row, int col) {
        return this.delegate.getStackInRowAndColumn(row, col);
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.cleanSize; i++) {
            setInventorySlotContents(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return this.delegate.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        this.delegate.markDirty();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.delegate.isUsableByPlayer(player);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        this.delegate.openInventory(player);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        this.delegate.closeInventory(player);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index >= 0 && index < this.cleanSize && this.delegate.isItemValidForSlot(index, stack);
    }

    @Override
    public int getField(int id) {
        return this.delegate.getField(id);
    }

    @Override
    public void setField(int id, int value) {
        this.delegate.setField(id, value);
    }

    @Override
    public int getFieldCount() {
        return this.delegate.getFieldCount();
    }

    @Override
    public String getName() {
        return this.delegate.getName();
    }

    @Override
    public boolean hasCustomName() {
        return this.delegate.hasCustomName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return this.delegate.getDisplayName();
    }

    @Override
    public void fillStackedContents(RecipeItemHelper helper) {
        for (int i = 0; i < this.cleanSize; i++) {
            helper.accountStack(getStackInSlot(i));
        }
    }

    @Override
    @Nullable
    public RecipeSelectionState retropolymorph$peekRecipeSelectionState() {
        if (this.delegate instanceof CraftingMatrixExtension) {
            return ((CraftingMatrixExtension) this.delegate).retropolymorph$peekRecipeSelectionState();
        }
        return null;
    }

    @Override
    public RecipeSelectionState retropolymorph$getOrCreateRecipeSelectionState() {
        if (this.delegate instanceof CraftingMatrixExtension) {
            return ((CraftingMatrixExtension) this.delegate).retropolymorph$getOrCreateRecipeSelectionState();
        }
        return new RecipeSelectionState();
    }

    @Override
    @Nullable
    public Container retropolymorph$getCraftingOwner() {
        if (this.delegate instanceof CraftingMatrixExtension) {
            return ((CraftingMatrixExtension) this.delegate).retropolymorph$getCraftingOwner();
        }
        return null;
    }
}
