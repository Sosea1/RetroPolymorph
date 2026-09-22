package dev.sosea1.retropolymorph.api;

/**
 * Declares who owns a machine recipe selection.
 *
 * <p>PLAYER_PROFILE is purely player-owned. OWNER is purely machine-owned and
 * never imports a player's global conflict preference. OWNER_WITH_PLAYER_PROFILE
 * keeps the authoritative choice on the machine (so unattended automation still
 * knows what to craft), while allowing the current player's stored conflict
 * preference to seed/replace that owner choice when the GUI is queried. SESSION
 * is intentionally ephemeral.</p>
 */
public enum MachineRecipePersistence {
    PLAYER_PROFILE,
    OWNER,
    OWNER_WITH_PLAYER_PROFILE,
    SESSION
}
