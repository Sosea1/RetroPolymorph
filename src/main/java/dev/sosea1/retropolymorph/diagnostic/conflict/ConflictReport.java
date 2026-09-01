package dev.sosea1.retropolymorph.diagnostic.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of one explicit conflict scan. */
public final class ConflictReport {

    private final int craftingRecipesScanned;
    private final List<CraftingConflictGroup> craftingCandidates;
    private final int furnaceRecipesScanned;
    private final List<FurnaceConflictPair> furnaceConflicts;

    ConflictReport(
            int craftingRecipesScanned,
            List<CraftingConflictGroup> craftingCandidates,
            int furnaceRecipesScanned,
            List<FurnaceConflictPair> furnaceConflicts) {
        this.craftingRecipesScanned = craftingRecipesScanned;
        this.craftingCandidates = Collections.unmodifiableList(
                new ArrayList<CraftingConflictGroup>(craftingCandidates));
        this.furnaceRecipesScanned = furnaceRecipesScanned;
        this.furnaceConflicts = Collections.unmodifiableList(
                new ArrayList<FurnaceConflictPair>(furnaceConflicts));
    }

    public int getCraftingRecipesScanned() {
        return this.craftingRecipesScanned;
    }

    public List<CraftingConflictGroup> getCraftingCandidates() {
        return this.craftingCandidates;
    }

    public int getFurnaceRecipesScanned() {
        return this.furnaceRecipesScanned;
    }

    public List<FurnaceConflictPair> getFurnaceConflicts() {
        return this.furnaceConflicts;
    }
}
