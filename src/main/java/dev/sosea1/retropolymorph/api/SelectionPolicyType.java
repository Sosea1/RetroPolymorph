package dev.sosea1.retropolymorph.api;

/**
 * Selects which default-choice policy should be applied to a selection context.
 *
 * <p>RECIPE is the normal policy for Forge recipes and custom crafting engines.
 * SMELTING is intentionally separate because legacy FurnaceRecipes has no
 * registered recipe ID to expose to users or modpack configuration.</p>
 */
public enum SelectionPolicyType {
    RECIPE,
    SMELTING
}
