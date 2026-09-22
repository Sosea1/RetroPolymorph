package dev.sosea1.retropolymorph.furnace;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Selection context for the vanilla TileEntityFurnace recipe engine. */
final class FurnaceContext implements SelectionContext {

    private static final int INPUT_SLOT = 0;

    private final Container container;
    private final IInventory furnace;
    private final Slot resultSlot;

    FurnaceContext(
            Container container,
            IInventory furnace,
            Slot resultSlot) {
        this.container = container;
        this.furnace = furnace;
        this.resultSlot = resultSlot;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public int getInputCount() {
        return 1;
    }

    @Override
    public ItemStack getInputStack(int index) {
        return index == 0 ? this.furnace.getStackInSlot(INPUT_SLOT) : ItemStack.EMPTY;
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        return FurnaceRecipeResolver.findOptions(
                FurnaceRecipes.instance(),
                this.furnace.getStackInSlot(INPUT_SLOT));
    }

    @Override
    public boolean select(String recipeKey, World world) {
        boolean selected = FurnaceRecipeResolver.selectLazy(
                FurnaceRecipes.instance(),
                this.furnace,
                this.furnace.getStackInSlot(INPUT_SLOT),
                recipeKey);
        if (selected) {
            this.furnace.markDirty();
        }
        return selected;
    }

    @Override
    public void clearSelection() {
        FurnaceSelectionState state = FurnaceSelectionStore.peek(this.furnace);
        if (state == null || !state.hasSelection()) {
            return;
        }
        state.clear();
        this.furnace.markDirty();
    }

    @Override
    public boolean retainSelectionWhenOptionsEmpty() {
        return this.furnace.getStackInSlot(INPUT_SLOT).isEmpty();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        FurnaceSelectionState state = FurnaceSelectionStore.peek(this.furnace);
        if (state == null) {
            return null;
        }

        String selected = state.getSelectedKey();
        ItemStack input = this.furnace.getStackInSlot(INPUT_SLOT);
        if (selected == null || input.isEmpty()) {
            return selected;
        }

        FurnaceRecipeResolver.resolve(FurnaceRecipes.instance(), input, state);
        String validated = state.getSelectedKey();
        if (validated == null) {
            this.furnace.markDirty();
        }
        return validated;
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        FurnaceSelectionState state = FurnaceSelectionStore.peek(this.furnace);
        if (recipeKey == null) {
            if (state != null) {
                state.clear();
            }
            return;
        }
        FurnaceSelectionStore.getOrCreate(this.furnace).setPersistentKey(recipeKey);
    }
}
