package com.sosea1.retropolymorph.compat.rftools;

import com.sosea1.retropolymorph.core.RecipeProbe;
import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.api.RecipeSelectionContext;
import com.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import com.sosea1.retropolymorph.api.SelectionScope;
import com.sosea1.retropolymorph.core.RecipeResolver;
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
    public com.sosea1.retropolymorph.api.SelectorPlacement getSelectorPlacement() {
        return com.sosea1.retropolymorph.api.SelectorPlacement.resultSlot(
                this.resultSlot.xPos,
                this.resultSlot.yPos,
                19,
                -53);
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
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }

        RFToolsPreviewSelection.set(this.inputSlots[0].inventory, recipeId);
        applyOutput(recipe);
        return true;
    }

    @Override
    public void clearSelection() {
        RFToolsPreviewSelection.set(this.inputSlots[0].inventory, null);
        refreshMatrix();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation selected = RFToolsPreviewSelection.get(this.inputSlots[0].inventory);
        return selected == null ? null : selected.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        RFToolsPreviewSelection.set(this.inputSlots[0].inventory, recipeId);
        if (recipeId != null) {
            IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
            if (recipe != null) {
                applyOutput(recipe);
            }
        }
    }

    @Override
    public SelectionPersistencePolicy getPersistencePolicy() {
        return SelectionPersistencePolicy.OWNER_ONLY;
    }

    @Override
    public SelectionScope getSelectionScope() {
        return SelectionScope.shared(this.inputSlots[0].inventory);
    }

    private void applyOutput(IRecipe recipe) {
        refreshMatrix();
        ItemStack output = RecipeProbe.craftingResult(recipe, this.matrix);
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
