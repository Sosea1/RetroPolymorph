package dev.sosea1.retropolymorph.api;

/**
 * Immutable policy describing how a selection surface participates in durable
 * player preferences and whether personal choices can override an active selection.
 */
public final class SelectionPersistencePolicy {

    /**
     * Standard crafting surfaces: remembers player preference per conflict,
     * but does not overwrite an already-selected recipe in an open session.
     */
    public static final SelectionPersistencePolicy PLAYER_PERSISTENT =
            new SelectionPersistencePolicy(true, false);

    /**
     * Opt-in surfaces where opening the container immediately reapplies the
     * player's stored preference over a current selection.
     */
    public static final SelectionPersistencePolicy PLAYER_PERSISTENT_OVERRIDE =
            new SelectionPersistencePolicy(true, true);

    /**
     * Shared machines or grids: does not store personal player preferences,
     * keeping only the machine/owner's authoritative state.
     */
    public static final SelectionPersistencePolicy OWNER_ONLY =
            new SelectionPersistencePolicy(false, false);

    private final boolean supportsPlayerPreferences;
    private final boolean playerPreferenceOverridesCurrent;

    public SelectionPersistencePolicy(
            boolean supportsPlayerPreferences,
            boolean playerPreferenceOverridesCurrent) {
        this.supportsPlayerPreferences = supportsPlayerPreferences;
        this.playerPreferenceOverridesCurrent = playerPreferenceOverridesCurrent;
    }

    public boolean supportsPlayerPreferences() {
        return this.supportsPlayerPreferences;
    }

    public boolean playerPreferenceOverridesCurrent() {
        return this.playerPreferenceOverridesCurrent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectionPersistencePolicy that = (SelectionPersistencePolicy) o;
        return this.supportsPlayerPreferences == that.supportsPlayerPreferences
                && this.playerPreferenceOverridesCurrent == that.playerPreferenceOverridesCurrent;
    }

    @Override
    public int hashCode() {
        int result = (this.supportsPlayerPreferences ? 1 : 0);
        result = 31 * result + (this.playerPreferenceOverridesCurrent ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SelectionPersistencePolicy{" +
                "supportsPlayerPreferences=" + this.supportsPlayerPreferences +
                ", playerPreferenceOverridesCurrent=" + this.playerPreferenceOverridesCurrent +
                '}';
    }
}
