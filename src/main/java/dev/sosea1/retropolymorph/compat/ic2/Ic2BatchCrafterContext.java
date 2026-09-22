package dev.sosea1.retropolymorph.compat.ic2;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingRules;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import dev.sosea1.retropolymorph.mixin.compat.ic2.Ic2BatchCrafterAccess;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Recipe selection for IC2's automatic, vanilla-recipe-backed Batch Crafter. */
final class Ic2BatchCrafterContext implements RecipeSelectionContext {

    private final Container container;
    private final Ic2BatchCrafterAccess access;
    private final InventoryCrafting matrix;
    private final Slot resultSlot;
    private final int buttonAnchorX;
    private final int buttonAnchorY;

    Ic2BatchCrafterContext(
            Container container,
            Ic2BatchCrafterAccess access,
            Slot resultSlot,
            int buttonAnchorX,
            int buttonAnchorY) {
        this.container = container;
        this.access = access;
        this.matrix = access.retropolymorph$getBatchCraftingMatrix();
        this.resultSlot = resultSlot;
        this.buttonAnchorX = buttonAnchorX;
        this.buttonAnchorY = buttonAnchorY;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        return this.matrix;
    }

    @Override
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public SelectorPlacement getSelectorPlacement() {
        return SelectorPlacement.resultSlot(this.buttonAnchorX, this.buttonAnchorY, 32, 6);
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        return CraftingRules.isRepairCombination(this.matrix)
                ? Collections.<IRecipe>emptyList()
                : RecipeResolver.findAllMatches(this.matrix, world);
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation id = recipe.getRegistryName();
        return id == null ? null : id.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        if (CraftingRules.isRepairCombination(this.matrix)) {
            return false;
        }

        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        IRecipe recipe = id == null ? null : ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }

        state().select(id);
        recalculate();
        return true;
    }

    @Override
    public void clearSelection() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        if (state == null || !state.hasSelection()) {
            return;
        }
        state.clear();
        recalculate();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        ResourceLocation id = state == null ? null : state.getSelectedRecipeId();
        return id == null ? null : id.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        RecipeSelectionState current = extension().retropolymorph$peekRecipeSelectionState();
        if (recipeKey == null) {
            if (current != null) {
                current.clear();
            }
            recalculate();
            return;
        }

        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id != null) {
            state().select(id);
            // The Batch Crafter GUI reads its cached recipe locally. Mirror the
            // authoritative selection into that cache so preview and real craft agree.
            recalculate();
        }
    }

    private CraftingMatrixExtension extension() {
        return (CraftingMatrixExtension) this.matrix;
    }

    private RecipeSelectionState state() {
        return extension().retropolymorph$getOrCreateRecipeSelectionState();
    }

    private void recalculate() {
        // IC2 keeps the old matching recipe otherwise, so force findRecipe().
        this.access.retropolymorph$setBatchRecipe(null);
        this.access.retropolymorph$recalculateBatchRecipe(-1);
    }
}
