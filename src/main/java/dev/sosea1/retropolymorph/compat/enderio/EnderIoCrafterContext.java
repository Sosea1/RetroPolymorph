package dev.sosea1.retropolymorph.compat.enderio;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.core.CraftingRecipeOptionCollector;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Selection context for Ender IO's ghost recipe grid. */
final class EnderIoCrafterContext implements RecipeSelectionContext {

    private static final int INPUTS = 9;

    private final Container container;
    private final TileEntity tile;
    private final IInventory ghostGrid;
    private final Slot resultSlot;

    EnderIoCrafterContext(
            Container container,
            TileEntity tile,
            IInventory ghostGrid,
            Slot resultSlot) {
        this.container = container;
        this.tile = tile;
        this.ghostGrid = ghostGrid;
        this.resultSlot = resultSlot;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        return copyGhostMatrix();
    }

    @Override
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public int getInputCount() {
        return INPUTS;
    }

    @Override
    public ItemStack getInputStack(int index) {
        return index >= 0 && index < INPUTS
                ? this.ghostGrid.getStackInSlot(index)
                : ItemStack.EMPTY;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        return RecipeResolver.findAllMatches(copyGhostMatrix(), world);
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        InventoryCrafting matrix = copyGhostMatrix();
        if (RecipeSelectionContext.isEmpty(matrix)) {
            return Collections.emptyList();
        }
        return CraftingRecipeOptionCollector.collect(
                this,
                matrix,
                RecipeResolver.findAllMatches(matrix, world));
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation id = recipe.getRegistryName();
        return id == null ? null : id.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id == null) {
            return false;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        InventoryCrafting matrix = copyGhostMatrix();
        if (recipe == null || !RecipeProbe.matches(recipe, matrix, world)) {
            return false;
        }

        EnderIoCrafterSelectionStore.setSelectedRecipeId(this.tile, id);
        EnderIoCrafterReflection.refreshOutput(this.tile);
        return true;
    }

    @Override
    public void clearSelection() {
        EnderIoCrafterSelectionStore.setSelectedRecipeId(this.tile, null);
        EnderIoCrafterReflection.refreshOutput(this.tile);
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation id = EnderIoCrafterSelectionStore.getSelectedRecipeId(this.tile);
        return id == null ? null : id.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        // The server owns the tile's persistent selection. Client-side preview
        // is synchronized by Ender IO itself; do not mutate fake local tile NBT.
    }

    @Override
    public dev.sosea1.retropolymorph.api.SelectorPlacement getSelectorPlacement() {
        // ContainerCrafter's real output is x=172 while its ghost recipe output
        // is x=90. Keep the selector visually attached to the recipe preview.
        return dev.sosea1.retropolymorph.api.SelectorPlacement.resultSlot(
                this.resultSlot.xPos,
                this.resultSlot.yPos,
                -82,
                0);
    }

    private InventoryCrafting copyGhostMatrix() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        for (int slot = 0; slot < INPUTS; slot++) {
            ItemStack stack = this.ghostGrid.getStackInSlot(slot);
            matrix.setInventorySlotContents(
                    slot,
                    stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return matrix;
    }

    private static final class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    }
}
