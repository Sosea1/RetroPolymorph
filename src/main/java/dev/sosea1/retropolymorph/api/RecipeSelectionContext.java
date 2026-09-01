package dev.sosea1.retropolymorph.api;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Crafting-specific extension of the common selection context.
 *
 * Recipe keys are opaque outside the owning context. Forge-backed contexts use
 * registry names; custom recipe engines may use their own stable key format.
 */
public interface RecipeSelectionContext extends SelectionContext {

    InventoryCrafting getRecipeMatrix();

    List<IRecipe> findAllMatches(World world);

    /**
     * Returns the key that should be sent over the wire for this recipe, or
     * null when this context cannot safely select that recipe.
     */
    @Nullable
    String getRecipeKey(IRecipe recipe);

    @Override
    default int getInputCount() {
        return getRecipeMatrix().getSizeInventory();
    }

    @Override
    default ItemStack getInputStack(int index) {
        return getRecipeMatrix().getStackInSlot(index);
    }

    @Override
    default List<RecipeOption> findOptions(World world) {
        InventoryCrafting matrix = getRecipeMatrix();
        if (isEmpty(matrix)) {
            return Collections.emptyList();
        }

        List<IRecipe> matches = findAllMatches(world);
        if (matches.size() <= 1) {
            return Collections.emptyList();
        }

        ArrayList<RecipeOption> options = new ArrayList<RecipeOption>(matches.size());
        for (IRecipe recipe : matches) {
            String key = getRecipeKey(recipe);
            if (!RecipeKey.isWireSafe(key)) {
                continue;
            }

            ItemStack output = recipe.getCraftingResult(matrix);
            if (!output.isEmpty()) {
                options.add(new RecipeOption(key, output.copy()));
            }
        }

        return options.size() <= 1 ? Collections.<RecipeOption>emptyList() : options;
    }

    static boolean isEmpty(InventoryCrafting matrix) {
        for (int slot = 0; slot < matrix.getSizeInventory(); slot++) {
            if (!matrix.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
