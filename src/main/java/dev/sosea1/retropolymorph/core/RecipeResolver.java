package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecipeResolver {

    private RecipeResolver() {
    }

    public static List<IRecipe> findAllMatches(InventoryCrafting matrix, World world) {
        if (matrix == null || matrix.isEmpty()) {
            return Collections.emptyList();
        }

        List<IRecipe> matches = new ArrayList<IRecipe>(4);

        for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            if (recipe.matches(matrix, world)) {
                matches.add(recipe);
            }
        }

        return matches.isEmpty() ? Collections.emptyList() : matches;
    }
}
