package dev.sosea1.retropolymorph.compat.rftools;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public final class RFToolsCrafterContext implements RecipeSelectionContext {

    private static final Container MIRROR_OWNER = new MirrorContainer();

    private final Container container;
    private final Slot[] inputSlots;
    private final Slot resultSlot;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);

    @Nullable
    private ResourceLocation selectedRecipeId;

    public RFToolsCrafterContext(
            Container container,
            Slot[] inputSlots,
            Slot resultSlot) {
        this.container = container;
        this.inputSlots = inputSlots;
        this.resultSlot = resultSlot;
        refreshMatrix();
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        refreshMatrix();
        return this.matrix;
    }

    @Override
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public int getButtonOffsetX() {
        return 19;
    }

    @Override
    public int getButtonOffsetY() {
        return -53;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        refreshMatrix();
        return RecipeResolver.findAllMatches(this.matrix, world);
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation recipeId = recipe.getRegistryName();
        return recipeId == null ? null : recipeId.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }

        refreshMatrix();
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null || !recipe.matches(this.matrix, world)) {
            return false;
        }

        this.selectedRecipeId = recipeId;
        applyOutput(recipe);
        return true;
    }

    @Override
    public void clearSelection() {
        this.selectedRecipeId = null;
        refreshMatrix();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        return this.selectedRecipeId == null ? null : this.selectedRecipeId.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        this.selectedRecipeId = recipeId;
        if (recipeId != null) {
            IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
            if (recipe != null) {
                applyOutput(recipe);
            }
        }
    }

    private void applyOutput(IRecipe recipe) {
        refreshMatrix();
        ItemStack output = recipe.getCraftingResult(this.matrix);
        this.resultSlot.putStack(output.copy());
    }

    private void refreshMatrix() {
        for (int slot = 0; slot < this.inputSlots.length; slot++) {
            ItemStack source = this.inputSlots[slot].getStack();
            ItemStack mirrored = this.matrix.getStackInSlot(slot);
            if (mirrored != source && !ItemStack.areItemStacksEqual(mirrored, source)) {
                this.matrix.setInventorySlotContents(slot, source.isEmpty() ? ItemStack.EMPTY : source.copy());
            }
        }
    }

    private static final class MirrorContainer extends Container {

        @Override
        public void onCraftMatrixChanged(IInventory inventory) {
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }
}
