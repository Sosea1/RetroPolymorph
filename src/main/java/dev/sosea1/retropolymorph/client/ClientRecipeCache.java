package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeOptions;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

/** Client-side input snapshot plus the last server-authoritative options. */
final class ClientRecipeCache {

    private SelectionContext context;
    private int stateToken;
    private ItemStack[] snapshot = new ItemStack[0];
    private List<RecipeOption> choices = Collections.emptyList();

    boolean refreshInputs(SelectionContext currentContext) {
        if (this.context == currentContext
                && this.stateToken == currentContext.getClientStateToken()
                && snapshotMatches(currentContext)) {
            return false;
        }

        this.context = currentContext;
        this.stateToken = currentContext.getClientStateToken();
        captureSnapshot(currentContext);
        return true;
    }

    boolean setChoices(List<RecipeOption> options, String selectedRecipeKey) {
        List<RecipeOption> sanitized = RecipeOptions.sanitizeAndLimit(
                options, selectedRecipeKey);
        if (sameChoices(this.choices, sanitized)) {
            return false;
        }
        this.choices = sanitized;
        return true;
    }

    void clearChoices() {
        this.choices = Collections.emptyList();
    }

    List<RecipeOption> getChoices() {
        return this.choices;
    }

    void invalidateInputs() {
        this.context = null;
        this.stateToken = 0;
    }


    private boolean snapshotMatches(SelectionContext currentContext) {
        int size = currentContext.getInputCount();
        if (this.snapshot.length != size) {
            return false;
        }

        for (int slot = 0; slot < size; slot++) {
            if (!ItemStack.areItemStacksEqual(
                    this.snapshot[slot],
                    currentContext.getInputStack(slot))) {
                return false;
            }
        }
        return true;
    }

    private void captureSnapshot(SelectionContext currentContext) {
        int size = currentContext.getInputCount();
        if (this.snapshot.length != size) {
            this.snapshot = new ItemStack[size];
        }

        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = currentContext.getInputStack(slot);
            this.snapshot[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
    }

    private static boolean sameChoices(List<RecipeOption> first, List<RecipeOption> second) {
        if (first == second) {
            return true;
        }
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            RecipeOption a = first.get(index);
            RecipeOption b = second.get(index);
            if (!a.getRecipeKey().equals(b.getRecipeKey())
                    || !ItemStack.areItemStacksEqual(a.getOutput(), b.getOutput())) {
                return false;
            }
        }
        return true;
    }
}
