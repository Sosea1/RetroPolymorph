package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionReason;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Result returned by {@link SelectionService#handle}.
 */
public final class SelectionServiceResult {

    private final boolean accepted;
    private final List<RecipeOption> options;
    private final String selectedRecipeKey;
    private final SelectionReason reason;
    private final boolean selectionChanged;

    public SelectionServiceResult(
            boolean accepted,
            List<RecipeOption> options,
            @Nullable String selectedRecipeKey,
            SelectionReason reason,
            boolean selectionChanged) {
        this.accepted = accepted;
        this.options = options == null
                ? Collections.<RecipeOption>emptyList()
                : Collections.unmodifiableList(new java.util.ArrayList<RecipeOption>(options));
        this.selectedRecipeKey = selectedRecipeKey;
        this.reason = reason;
        this.selectionChanged = selectionChanged;
    }

    public boolean isAccepted() {
        return this.accepted;
    }

    public List<RecipeOption> getOptions() {
        return this.options;
    }

    @Nullable
    public String getSelectedRecipeKey() {
        return this.selectedRecipeKey;
    }

    public SelectionReason getReason() {
        return this.reason;
    }

    public boolean isSelectionChanged() {
        return this.selectionChanged;
    }

    @Override
    public String toString() {
        return "SelectionServiceResult{" +
                "accepted=" + this.accepted +
                ", options=" + this.options.size() +
                ", selected='" + this.selectedRecipeKey + '\'' +
                ", reason=" + this.reason +
                ", changed=" + this.selectionChanged +
                '}';
    }
}
