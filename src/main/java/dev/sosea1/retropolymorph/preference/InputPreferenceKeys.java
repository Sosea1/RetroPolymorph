package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Recipe-aware aliases used by the fast persistent preference index. */
public final class InputPreferenceKeys {

    private InputPreferenceKeys() {
    }

    /**
     * Keys worth probing before a full conflict query. Positional is more
     * specific and therefore always wins over the shapeless multiset alias.
     */
    public static List<String> lookupKeys(@Nullable SelectionContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        ArrayList<String> keys = new ArrayList<String>(2);
        addUnique(keys, InputFingerprint.create(context));
        addUnique(keys, InputFingerprint.createUnordered(context));
        return keys;
    }

    /**
     * Aliases that are safe to persist for one already-authorized recipe.
     * Translation is built into the positional key. A mirrored positional key
     * is added only when the live recipe actually matches that reflected input.
     * Unordered aliases are limited to the standard Forge/vanilla shapeless
     * recipe families where arbitrary slot permutation is part of the contract.
     */
    public static List<String> storageKeys(
            @Nullable SelectionContext context,
            @Nullable IRecipe recipe,
            @Nullable World world) {
        if (context == null) {
            return Collections.emptyList();
        }

        ArrayList<String> keys = new ArrayList<String>(3);
        addUnique(keys, InputFingerprint.create(context));

        if (recipe == null || !(context instanceof RecipeSelectionContext)) {
            return keys;
        }

        if (isKnownShapeless(recipe)) {
            addUnique(keys, InputFingerprint.createUnordered(context));
            return keys;
        }

        if (world != null) {
            RecipeSelectionContext recipeContext = (RecipeSelectionContext) context;
            InventoryCrafting mirror = InputFingerprint.createHorizontalMirrorMatrix(recipeContext);
            String mirrorKey = InputFingerprint.createHorizontalMirror(context);
            if (mirror != null
                    && mirrorKey != null
                    && RecipeProbe.matches(recipe, mirror, world)) {
                addUnique(keys, mirrorKey);
            }
        }
        return keys;
    }

    private static boolean isKnownShapeless(IRecipe recipe) {
        return recipe instanceof ShapelessRecipes || recipe instanceof ShapelessOreRecipe;
    }

    private static void addUnique(List<String> keys, @Nullable String key) {
        if (key != null && !keys.contains(key)) {
            keys.add(key);
        }
    }
}
