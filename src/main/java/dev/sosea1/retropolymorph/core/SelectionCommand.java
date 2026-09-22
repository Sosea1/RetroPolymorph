package dev.sosea1.retropolymorph.core;

import javax.annotation.Nullable;

/**
 * Transport-independent command requesting an action on a selection context.
 */
public final class SelectionCommand {

    private static final SelectionCommand QUERY = new SelectionCommand(SelectionAction.QUERY, null);
    private static final SelectionCommand CLEAR = new SelectionCommand(SelectionAction.CLEAR, null);

    private final SelectionAction action;
    private final String recipeKey;

    private SelectionCommand(SelectionAction action, @Nullable String recipeKey) {
        this.action = action;
        this.recipeKey = recipeKey;
    }

    public static SelectionCommand query() {
        return QUERY;
    }

    public static SelectionCommand clear() {
        return CLEAR;
    }

    public static SelectionCommand select(String recipeKey) {
        return new SelectionCommand(SelectionAction.SELECT, recipeKey);
    }

    public SelectionAction getAction() {
        return this.action;
    }

    public boolean isQuery() {
        return this.action == SelectionAction.QUERY;
    }

    public boolean isSelect() {
        return this.action == SelectionAction.SELECT;
    }

    public boolean isClear() {
        return this.action == SelectionAction.CLEAR;
    }

    @Nullable
    public String getRecipeKey() {
        return this.recipeKey;
    }

    @Override
    public String toString() {
        return "SelectionCommand{" +
                "action=" + this.action +
                (this.recipeKey != null ? ", recipeKey='" + this.recipeKey + '\'' : "") +
                '}';
    }
}
