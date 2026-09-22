package dev.sosea1.retropolymorph.api;

import dev.sosea1.retropolymorph.machine.MachineRecipeSelectionAdapter;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * Public integration API for Retro Polymorph.
 *
 * <p>Third-party mods and addons can register focused recipe-selection adapters
 * during startup. Higher priorities are probed first; equal priorities preserve
 * registration order.</p>
 *
 * <p>Use {@link #PRIORITY_NORMAL} for normal addons.
 * {@link #PRIORITY_OVERRIDE} is reserved for integrations that intentionally
 * replace a built-in adapter, while {@link #PRIORITY_FALLBACK} is intended for
 * generic last-resort scanners.</p>
 */
public final class RetroPolymorphAPI {

    /** First public addon API revision. */
    public static final int API_VERSION = 1;

    /** Last-resort fallback tier for generic slot scanners. */
    public static final int PRIORITY_FALLBACK = 0;

    /** Standard tier for third-party addon adapters. */
    public static final int PRIORITY_NORMAL = 500;

    /**
     * Explicit override tier for addons that intentionally replace a built-in integration.
     */
    public static final int PRIORITY_OVERRIDE = 2000;

    private RetroPolymorphAPI() {
    }

    /**
     * Registers a recipe selection adapter using the standard addon priority ({@link #PRIORITY_NORMAL}).
     *
     * @param id unique identifier for this adapter
     * @param adapter the adapter implementation
     */
    public static void registerAdapter(ResourceLocation id, RecipeSelectionAdapter adapter) {
        registerAdapter(id, PRIORITY_NORMAL, adapter);
    }

    /**
     * Registers a recipe selection adapter with an explicit priority tier.
     *
     * @param id unique identifier for this adapter
     * @param priority probe priority (higher numbers are queried earlier)
     * @param adapter the adapter implementation
     */
    public static void registerAdapter(ResourceLocation id, int priority, RecipeSelectionAdapter adapter) {
        RecipeSelectionAdapters.register(id, priority, adapter);
    }

    /**
     * Registers an opt-in machine/processing recipe engine using the standard addon priority ({@link #PRIORITY_NORMAL}).
     * Recognized custom engines block generic slot guessing even when their live surface cannot be opened safely.
     *
     * @param id unique identifier for this adapter
     * @param adapter the machine recipe adapter implementation
     */
    public static void registerMachineAdapter(ResourceLocation id, MachineRecipeAdapter adapter) {
        registerMachineAdapter(id, PRIORITY_NORMAL, adapter);
    }

    /**
     * Registers an opt-in machine/processing recipe engine with an explicit priority tier.
     * Recognized custom engines block generic slot guessing even when their live surface cannot be opened safely.
     *
     * @param id unique identifier for this adapter
     * @param priority probe priority (higher numbers are queried earlier)
     * @param adapter the machine recipe adapter implementation
     */
    public static void registerMachineAdapter(
            ResourceLocation id,
            int priority,
            MachineRecipeAdapter adapter) {
        registerAdapter(id, priority, new MachineRecipeSelectionAdapter(adapter));
    }

    /**
     * Returns an unmodifiable view of all registered adapters in descending priority order.
     */
    public static List<RecipeSelectionAdapterInfo> getRegisteredAdapters() {
        return RecipeSelectionAdapters.getRegisteredAdapters();
    }

    /**
     * Registers a modpack/addon default recipe preference priority.
     * Player choices always take precedence over configured defaults.
     */
    public static void setRecipePriority(ResourceLocation recipeId, int priority) {
        RecipePreferencePolicy.setPriority(recipeId, priority);
    }
}
