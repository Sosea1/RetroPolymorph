package dev.sosea1.retropolymorph.compat.extendedcrafting;

import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.core.CraftingContext;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingRules;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Recipe-selection context for Extended Crafting's table recipe engine. */
final class ExtendedTableContext implements RecipeSelectionContext {

    private static final String KEY_PREFIX = "extendedcrafting:table/";

    private final Container container;
    private final InventoryCrafting matrix;
    private final Slot resultSlot;
    private final ExtendedTableRecipeManagerBridge manager;
    private final ExtendedTableMatrixExtension extension;
    private final CraftingContext forgeFallback;

    ExtendedTableContext(
            Container container,
            InventoryCrafting matrix,
            Slot resultSlot,
            ExtendedTableRecipeManagerBridge manager) {
        this.container = container;
        this.matrix = matrix;
        this.resultSlot = resultSlot;
        this.manager = manager;
        this.extension = (ExtendedTableMatrixExtension) matrix;
        this.forgeFallback = new CraftingContext(container, matrix, resultSlot);
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

        List<IRecipe> customMatches = findCustomMatches(world);
        if (!customMatches.isEmpty()) {
            return customMatches;
        }

        // Extended Crafting 3x3 table fallback for standard Forge recipes
        if (this.matrix.getSizeInventory() != 9) {
            return Collections.emptyList();
        }

        ItemStack defaultResult = this.manager.retropolymorph$findTableResult(this.matrix, world);
        if (defaultResult.isEmpty()) {
            return Collections.emptyList();
        }

        return RecipeResolver.findAllMatches(this.matrix, world);
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        int index = identityIndexOf(this.manager.retropolymorph$getTableRecipes(), recipe);
        if (index >= 0) {
            return KEY_PREFIX + index;
        }
        return this.forgeFallback.getRecipeKey(recipe);
    }

    @Override
    public boolean select(String recipeKey, World world) {
        int index = parseIndex(recipeKey);
        if (index >= 0) {
            return selectCustom(index, world);
        }

        if (CraftingRules.isRepairCombination(this.matrix)) {
            return false;
        }

        return this.forgeFallback.select(recipeKey, world);
    }

    @Override
    public void clearSelection() {
        boolean changed = false;
        if (this.extension.retropolymorph$getSelectedTableRecipeIndex() >= 0) {
            this.extension.retropolymorph$clearTableRecipe();
            changed = true;
        }

        RecipeSelectionState forgeState = forgeState();
        if (forgeState != null && forgeState.hasSelection()) {
            forgeState.clear();
            changed = true;
        }

        if (changed) {
            refreshOutput();
        }
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        if (CraftingRules.isRepairCombination(this.matrix)) {
            clearSelectionWithoutRefresh();
            return null;
        }

        int index = this.extension.retropolymorph$getSelectedTableRecipeIndex();
        IRecipe selected = this.extension.retropolymorph$getSelectedTableRecipe();
        List<IRecipe> recipes = this.manager.retropolymorph$getTableRecipes();
        if (index >= 0 || selected != null) {
            if (index < 0 || selected == null || index >= recipes.size() || recipes.get(index) != selected) {
                this.extension.retropolymorph$clearTableRecipe();
            } else {
                return KEY_PREFIX + index;
            }
        }

        return this.forgeFallback.getSelectedRecipeKey();
    }

    private boolean selectCustom(int index, World world) {
        if (CraftingRules.isRepairCombination(this.matrix)) {
            return false;
        }

        List<IRecipe> recipes = this.manager.retropolymorph$getTableRecipes();
        if (index >= recipes.size()) {
            return false;
        }

        IRecipe recipe = recipes.get(index);
        if (recipe == null || !recipe.matches(this.matrix, world)) {
            return false;
        }

        int previousIndex = this.extension.retropolymorph$getSelectedTableRecipeIndex();
        IRecipe previousRecipe = this.extension.retropolymorph$getSelectedTableRecipe();
        this.extension.retropolymorph$selectTableRecipe(index, recipe);
        refreshOutput();

        if (this.extension.retropolymorph$wasTableResolutionObserved()) {
            clearForgeSelectionWithoutRefresh();
            return true;
        }

        if (previousIndex >= 0 && previousRecipe != null) {
            this.extension.retropolymorph$selectTableRecipe(previousIndex, previousRecipe);
        } else {
            this.extension.retropolymorph$clearTableRecipe();
        }
        refreshOutput();
        return false;
    }

    private List<IRecipe> findCustomMatches(World world) {
        List<IRecipe> recipes = this.manager.retropolymorph$getTableRecipes();
        if (recipes.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<IRecipe> matches = new ArrayList<IRecipe>();
        for (IRecipe recipe : recipes) {
            if (recipe != null && recipe.matches(this.matrix, world)) {
                matches.add(recipe);
            }
        }
        return matches;
    }

    private void refreshOutput() {
        this.container.onCraftMatrixChanged(this.matrix);
    }

    @Nullable
    private RecipeSelectionState forgeState() {
        if (!(this.matrix instanceof CraftingMatrixExtension)) {
            return null;
        }
        return ((CraftingMatrixExtension) this.matrix).retropolymorph$peekRecipeSelectionState();
    }

    private void clearForgeSelectionWithoutRefresh() {
        RecipeSelectionState state = forgeState();
        if (state != null && state.hasSelection()) {
            state.clear();
        }
    }

    private void clearSelectionWithoutRefresh() {
        this.extension.retropolymorph$clearTableRecipe();
        clearForgeSelectionWithoutRefresh();
    }

    private static int identityIndexOf(List<IRecipe> recipes, IRecipe target) {
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index) == target) {
                return index;
            }
        }
        return -1;
    }

    private static int parseIndex(String recipeKey) {
        if (recipeKey == null || !recipeKey.startsWith(KEY_PREFIX)) {
            return -1;
        }

        String encoded = recipeKey.substring(KEY_PREFIX.length());
        if (encoded.isEmpty()) {
            return -1;
        }
        try {
            int index = Integer.parseInt(encoded);
            return index < 0 ? -1 : index;
        } catch (NumberFormatException invalidIndex) {
            return -1;
        }
    }
}
