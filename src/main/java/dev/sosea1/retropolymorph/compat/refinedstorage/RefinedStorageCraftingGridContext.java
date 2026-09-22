package dev.sosea1.retropolymorph.compat.refinedstorage;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingRules;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
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

/**
 * Refined Storage 1.12 Crafting Grid context.
 *
 * RS owns the actual craft/shift-craft/network extraction semantics. We only
 * select which Forge IRecipe backs its cached currentRecipe and then ask the
 * grid to run its native matrix refresh.
 */
final class RefinedStorageCraftingGridContext implements RecipeSelectionContext {

    private final Container container;
    private final RefinedStorageCraftingGridAccess access;
    private final InventoryCrafting matrix;
    private final Slot resultSlot;

    RefinedStorageCraftingGridContext(
            Container container,
            RefinedStorageCraftingGridAccess access,
            InventoryCrafting matrix,
            Slot resultSlot) {
        this.container = container;
        this.access = access;
        this.matrix = matrix;
        this.resultSlot = resultSlot;
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
    public List<IRecipe> findAllMatches(World world) {
        if (CraftingRules.isRepairCombination(this.matrix)) {
            return Collections.emptyList();
        }
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
        if (CraftingRules.isRepairCombination(this.matrix)) {
            return false;
        }

        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        if (recipeId == null) {
            return false;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeId);
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }

        state().select(recipeId);
        refreshNativeRecipe();
        return true;
    }

    @Override
    public void clearSelection() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        if (state == null || !state.hasSelection()) {
            return;
        }
        state.clear();
        refreshNativeRecipe();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        RecipeSelectionState state = extension().retropolymorph$peekRecipeSelectionState();
        ResourceLocation selected = state == null ? null : state.getSelectedRecipeId();
        return selected == null ? null : selected.toString();
    }

    /**
     * RS 1.12 stores both its matrix and currentRecipe on the grid/network node,
     * not per open container. Persisted per-player preferences would therefore
     * make two players silently fight over one shared output. Explicit choices
     * still work, but the generic durable player preference layer is disabled
     * for this shared context.
     */
    @Override
    public SelectionPersistencePolicy getPersistencePolicy() {
        return SelectionPersistencePolicy.OWNER_ONLY;
    }

    private void refreshNativeRecipe() {
        RefinedStorageSelectionRefresh.request(this.matrix);
        this.access.retropolymorph$refreshRefinedStorageCraftingMatrix();
    }

    private RecipeSelectionState state() {
        return extension().retropolymorph$getOrCreateRecipeSelectionState();
    }

    private CraftingMatrixExtension extension() {
        return (CraftingMatrixExtension) this.matrix;
    }
}
