package dev.sosea1.retropolymorph.compat.rftools;

import dev.sosea1.retropolymorph.core.RecipeProbe;
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

/** Forge-recipe context for RFTools Control's persistent manual Workbench. */
final class RFToolsWorkbenchContext implements RecipeSelectionContext {

    private static final Container MIRROR_OWNER = new MirrorContainer();

    private final Container container;
    private final RFToolsWorkbenchSelectionExtension extension;
    private final Slot[] inputSlots;
    private final Slot resultSlot;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);

    RFToolsWorkbenchContext(
            Container container,
            RFToolsWorkbenchSelectionExtension extension,
            Slot[] inputSlots,
            Slot resultSlot) {
        this.container = container;
        this.extension = extension;
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
    public List<IRecipe> findAllMatches(World world) {
        refreshMatrix();
        return RecipeResolver.findAllMatches(this.matrix, world);
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

        refreshMatrix();
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }

        ResourceLocation previous = this.extension.retropolymorph$getWorkbenchRecipeId();
        this.extension.retropolymorph$setWorkbenchRecipeId(id);
        this.extension.retropolymorph$refreshWorkbenchRecipe();
        if (this.extension.retropolymorph$wasWorkbenchSelectionObserved()) {
            return true;
        }

        this.extension.retropolymorph$setWorkbenchRecipeId(previous);
        this.extension.retropolymorph$refreshWorkbenchRecipe();
        return false;
    }

    @Override
    public void clearSelection() {
        if (this.extension.retropolymorph$getWorkbenchRecipeId() == null) {
            return;
        }
        this.extension.retropolymorph$setWorkbenchRecipeId(null);
        this.extension.retropolymorph$refreshWorkbenchRecipe();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation id = this.extension.retropolymorph$getWorkbenchRecipeId();
        return id == null ? null : id.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        this.extension.retropolymorph$setWorkbenchRecipeId(RecipeKey.parseForgeId(recipeKey));
    }

    private void refreshMatrix() {
        for (int index = 0; index < this.inputSlots.length; index++) {
            ItemStack source = this.inputSlots[index].getStack();
            ItemStack mirrored = this.matrix.getStackInSlot(index);
            if (!ItemStack.areItemStacksEqual(mirrored, source)) {
                this.matrix.setInventorySlotContents(
                        index,
                        source.isEmpty() ? ItemStack.EMPTY : source.copy());
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
