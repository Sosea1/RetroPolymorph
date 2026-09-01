package dev.sosea1.retropolymorph.diagnostic.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Recipes with the same normalized ingredient signature and different outputs. */
public final class CraftingConflictGroup {

    private final String inputDescription;
    private final List<String> recipes;

    CraftingConflictGroup(String inputDescription, List<String> recipes) {
        this.inputDescription = inputDescription;
        this.recipes = Collections.unmodifiableList(new ArrayList<String>(recipes));
    }

    public String getInputDescription() {
        return this.inputDescription;
    }

    public List<String> getRecipes() {
        return this.recipes;
    }
}
