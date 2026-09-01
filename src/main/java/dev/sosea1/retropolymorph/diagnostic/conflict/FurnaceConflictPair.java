package dev.sosea1.retropolymorph.diagnostic.conflict;

/** One exact legacy-smelting overlap with different outputs. */
public final class FurnaceConflictPair {

    private final String inputDescription;
    private final String first;
    private final String second;

    FurnaceConflictPair(String inputDescription, String first, String second) {
        this.inputDescription = inputDescription;
        this.first = first;
        this.second = second;
    }

    public String getInputDescription() {
        return this.inputDescription;
    }

    public String getFirst() {
        return this.first;
    }

    public String getSecond() {
        return this.second;
    }
}
