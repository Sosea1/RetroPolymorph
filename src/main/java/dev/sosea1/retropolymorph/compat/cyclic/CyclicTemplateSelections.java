package dev.sosea1.retropolymorph.compat.cyclic;

import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Bounded, tile-owned recipe choices for Cyclic's nine-slot template grid. */
public final class CyclicTemplateSelections {

    private static final String TAG = "RetroPolymorphTemplates";
    private static final String GRID_TAG = "Grid";
    private static final String RECIPE_TAG = "Recipe";
    private static final int GRID_SIZE = 9;
    private static final int MAX_TEMPLATES = 64;

    private final List<Entry> entries = new ArrayList<Entry>();

    @Nullable
    public ResourceLocation lookup(ItemStack[] grid) {
        if (!isValidGrid(grid)) {
            return null;
        }
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            Entry entry = this.entries.get(i);
            if (matches(entry.grid, grid)) {
                return entry.recipeId;
            }
        }
        return null;
    }

    public void remember(ItemStack[] grid, ResourceLocation recipeId) {
        if (!isValidGrid(grid) || recipeId == null) {
            return;
        }
        forget(grid);
        if (this.entries.size() == MAX_TEMPLATES) {
            this.entries.remove(0);
        }
        this.entries.add(new Entry(copyGrid(grid), recipeId));
    }

    public void forget(ItemStack[] grid) {
        if (!isValidGrid(grid)) {
            return;
        }
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            if (matches(this.entries.get(i).grid, grid)) {
                this.entries.remove(i);
            }
        }
    }

    public void writeToNBT(NBTTagCompound parent) {
        if (parent == null) {
            return;
        }
        if (this.entries.isEmpty()) {
            parent.removeTag(TAG);
            return;
        }
        NBTTagList list = new NBTTagList();
        for (Entry entry : this.entries) {
            NBTTagCompound saved = new NBTTagCompound();
            saved.setString(RECIPE_TAG, entry.recipeId.toString());
            NBTTagList stacks = new NBTTagList();
            for (ItemStack stack : entry.grid) {
                stacks.appendTag(stack.isEmpty()
                        ? new NBTTagCompound()
                        : stack.writeToNBT(new NBTTagCompound()));
            }
            saved.setTag(GRID_TAG, stacks);
            list.appendTag(saved);
        }
        parent.setTag(TAG, list);
    }

    public void readFromNBT(NBTTagCompound parent) {
        this.entries.clear();
        if (parent == null) {
            return;
        }
        NBTTagList list = parent.getTagList(TAG, 10);
        for (int i = 0; i < list.tagCount() && i < MAX_TEMPLATES; i++) {
            NBTTagCompound saved = list.getCompoundTagAt(i);
            ResourceLocation recipeId = RecipeKey.parseForgeId(saved.getString(RECIPE_TAG));
            NBTTagList stacks = saved.getTagList(GRID_TAG, 10);
            if (recipeId == null || stacks.tagCount() != GRID_SIZE) {
                continue;
            }
            ItemStack[] grid = new ItemStack[GRID_SIZE];
            for (int slot = 0; slot < GRID_SIZE; slot++) {
                grid[slot] = new ItemStack(stacks.getCompoundTagAt(slot));
            }
            remember(grid, recipeId);
        }
    }

    private static boolean isValidGrid(ItemStack[] grid) {
        if (grid == null || grid.length != GRID_SIZE) {
            return false;
        }
        for (ItemStack stack : grid) {
            if (stack != null && !stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack[] copyGrid(ItemStack[] grid) {
        ItemStack[] copy = new ItemStack[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = grid[i];
            if (stack == null || stack.isEmpty()) {
                copy[i] = ItemStack.EMPTY;
            } else {
                copy[i] = stack.copy();
                copy[i].setCount(1);
            }
        }
        return copy;
    }

    private static boolean matches(ItemStack[] saved, ItemStack[] current) {
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack before = saved[i];
            ItemStack after = current[i];
            if (after == null) {
                after = ItemStack.EMPTY;
            }
            if (before.isEmpty() != after.isEmpty()) {
                return false;
            }
            if (!before.isEmpty() && (!ItemStack.areItemsEqual(before, after)
                    || !ItemStack.areItemStackTagsEqual(before, after))) {
                return false;
            }
        }
        return true;
    }

    private static final class Entry {
        private final ItemStack[] grid;
        private final ResourceLocation recipeId;

        private Entry(ItemStack[] grid, ResourceLocation recipeId) {
            this.grid = grid;
            this.recipeId = recipeId;
        }
    }
}
