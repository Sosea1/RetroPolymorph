package dev.sosea1.retropolymorph.compat.mekanism;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.core.CraftingRecipeOptionCollector;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
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

/** Manual-grid selection context for Mekanism's Formulaic Assemblicator. */
final class MekanismFormulaicAssemblicatorContext implements RecipeSelectionContext {

    private final Container container;
    private final TileEntity tile;
    private final MekanismFormulaicTileAccess access;
    private final List<Slot> inputSlots;
    private final Slot formulaSlot;
    private final Slot resultAnchor;

    MekanismFormulaicAssemblicatorContext(
            Container container,
            TileEntity tile,
            MekanismFormulaicTileAccess access,
            List<Slot> inputSlots,
            Slot formulaSlot,
            Slot resultAnchor) {
        this.container = container;
        this.tile = tile;
        this.access = access;
        this.inputSlots = inputSlots;
        this.formulaSlot = formulaSlot;
        this.resultAnchor = resultAnchor;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        return copyMatrix();
    }

    @Override
    public Slot getResultSlot() {
        return this.resultAnchor;
    }

    @Override
    public int getInputCount() {
        return 9;
    }

    @Override
    public ItemStack getInputStack(int index) {
        if (index < 0 || index >= this.inputSlots.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.inputSlots.get(index).getStack();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public int getClientStateToken() {
        // Mekanism creates/validates RecipeFormula server-side. The formula
        // inventory slot itself is synchronized to the client, so use its
        // presence as the cheap lifecycle token for insert/remove transitions.
        return this.formulaSlot.getHasStack() ? 1 : 0;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        if (this.access.retropolymorph$hasFormula()) {
            return Collections.emptyList();
        }
        return RecipeResolver.findAllMatches(copyMatrix(), world);
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        if (this.access.retropolymorph$hasFormula()) {
            return Collections.emptyList();
        }
        InventoryCrafting matrix = copyMatrix();
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
        if (this.access.retropolymorph$hasFormula()) {
            return false;
        }
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id == null) {
            return false;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        InventoryCrafting snapshot = copyMatrix();
        if (recipe == null || !RecipeProbe.matches(recipe, snapshot, world)) {
            return false;
        }

        MekanismFormulaicSelectionStore.writeSelectedRecipeId(this.tile, id);
        seedNativeMatrix(id);
        this.access.retropolymorph$invalidateAndRecalculateRecipe();
        return true;
    }

    @Override
    public void clearSelection() {
        MekanismFormulaicSelectionStore.writeSelectedRecipeId(this.tile, null);
        InventoryCrafting nativeMatrix = this.access.retropolymorph$getFormulaicDummyMatrix();
        if (nativeMatrix instanceof dev.sosea1.retropolymorph.core.CraftingMatrixExtension) {
            dev.sosea1.retropolymorph.core.RecipeSelectionState state =
                    ((dev.sosea1.retropolymorph.core.CraftingMatrixExtension) nativeMatrix)
                            .retropolymorph$peekRecipeSelectionState();
            if (state != null) {
                state.clear();
            }
        }
        this.access.retropolymorph$invalidateAndRecalculateRecipe();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation id = MekanismFormulaicSelectionStore.getSelectedRecipeId(this.tile);
        return id == null ? null : id.toString();
    }

    @Override
    public SelectionPersistencePolicy getPersistencePolicy() {
        // The selected recipe is shared machine state and already persists in
        // tile ForgeData. Per-player durable preferences would make two users
        // silently fight over one automatic crafter.
        return SelectionPersistencePolicy.OWNER_ONLY;
    }

    @Override
    public SelectorPlacement getSelectorPlacement() {
        Slot result = getResultSlot();
        if (result != null) {
            return SelectorPlacement.resultSlot(result.xPos, result.yPos, -26, 8);
        }
        return SelectorPlacement.hidden();
    }

    private void seedNativeMatrix(@Nullable ResourceLocation id) {
        InventoryCrafting nativeMatrix = this.access.retropolymorph$getFormulaicDummyMatrix();
        if (nativeMatrix != null) {
            RecipeSelectionSeeder.seed(nativeMatrix, id);
        }
    }

    private InventoryCrafting copyMatrix() {
        InventoryCrafting matrix = new InventoryCrafting(new DummyContainer(), 3, 3);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = getInputStack(slot);
            matrix.setInventorySlotContents(
                    slot,
                    stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
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
