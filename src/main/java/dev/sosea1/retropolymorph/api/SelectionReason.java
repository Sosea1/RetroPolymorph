package dev.sosea1.retropolymorph.api;

/**
 * Architectural reason explaining why a recipe selection was chosen.
 */
public enum SelectionReason {

    /** The context already had an active valid selection that was not overridden. */
    CURRENT_CONTEXT(0),

    /** Restored from the player's persisted preference for this conflict fingerprint. */
    PLAYER_PREFERENCE(1),

    /** Selected by an exact configured recipe priority rule or addon API rule. */
    EXACT_RECIPE_POLICY(2),

    /** Selected by the ordered preferred-mods list. */
    MOD_PRIORITY(3),

    /** Automatically selected the first modded Forge recipe over vanilla natural default. */
    AUTOMATIC_MODDED(4),

    /** No explicit selection needed; native Forge / machine default order remains authoritative. */
    NATIVE_DEFAULT(5),

    /** Explicitly chosen by the player via direct UI interaction (click, keyboard, wheel). */
    PLAYER_SELECTION(6);

    private final int wireId;

    SelectionReason(int wireId) {
        this.wireId = wireId;
    }

    public int getWireId() {
        return this.wireId;
    }

    public static SelectionReason fromWireId(int id) {
        for (SelectionReason reason : values()) {
            if (reason.wireId == id) {
                return reason;
            }
        }
        return NATIVE_DEFAULT;
    }
}
