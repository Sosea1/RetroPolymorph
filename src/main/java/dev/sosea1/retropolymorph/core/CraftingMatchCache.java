package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

/**
 * Small exact-input MRU cache inspired by modern Polymorph's RecipeCache.
 *
 * <p>1.12.2 permits unusually dynamic custom IRecipe implementations, including
 * recipes whose matches() result can depend on world state. To avoid freezing a
 * dynamic match result across ticks, entries are intentionally valid only for
 * the world tick in which they were computed. This still coalesces duplicate
 * scans caused by one logical crafting update without assuming recipe purity.</p>
 */
final class CraftingMatchCache {

    private final Entry[] entries;

    CraftingMatchCache(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        this.entries = new Entry[size];
    }

    @Nullable
    List<IRecipe> get(InventoryCrafting matrix, World world) {
        long worldTick = world.getTotalWorldTime();
        for (int index = 0; index < this.entries.length; index++) {
            Entry entry = this.entries[index];
            if (entry != null && entry.matches(matrix, world, worldTick)) {
                moveToFront(index);
                return entry.recipes;
            }
        }
        return null;
    }

    void put(InventoryCrafting matrix, World world, List<IRecipe> recipes) {
        ItemStack[] snapshot = new ItemStack[matrix.getSizeInventory()];
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack stack = matrix.getStackInSlot(slot);
            snapshot[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }

        if (this.entries.length > 1) {
            System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
        }
        this.entries[0] = new Entry(
                snapshot,
                matrix.getWidth(),
                matrix.getHeight(),
                world,
                world.getTotalWorldTime(),
                recipes);
    }

    void clear() {
        Arrays.fill(this.entries, null);
    }

    private void moveToFront(int index) {
        if (index <= 0) {
            return;
        }
        Entry hit = this.entries[index];
        System.arraycopy(this.entries, 0, this.entries, 1, index);
        this.entries[0] = hit;
    }

    private static final class Entry {

        private final ItemStack[] snapshot;
        private final int width;
        private final int height;
        private final WeakReference<World> world;
        private final long worldTick;
        private final List<IRecipe> recipes;

        private Entry(
                ItemStack[] snapshot,
                int width,
                int height,
                World world,
                long worldTick,
                List<IRecipe> recipes) {
            this.snapshot = snapshot;
            this.width = width;
            this.height = height;
            this.world = new WeakReference<World>(world);
            this.worldTick = worldTick;
            this.recipes = recipes;
        }

        private boolean matches(InventoryCrafting matrix, World currentWorld, long currentWorldTick) {
            if (this.world.get() != currentWorld
                    || this.worldTick != currentWorldTick
                    || matrix.getWidth() != this.width
                    || matrix.getHeight() != this.height
                    || matrix.getSizeInventory() != this.snapshot.length) {
                return false;
            }

            for (int slot = 0; slot < this.snapshot.length; slot++) {
                if (!ItemStack.areItemStacksEqual(
                        this.snapshot[slot],
                        matrix.getStackInSlot(slot))) {
                    return false;
                }
            }
            return true;
        }
    }
}
