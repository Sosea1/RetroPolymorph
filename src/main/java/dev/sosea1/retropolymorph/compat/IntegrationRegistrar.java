package dev.sosea1.retropolymorph.compat;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RetroPolymorphAPI;
import net.minecraft.util.ResourceLocation;

/**
 * Self-contained registrar for an individual mod integration.
 */
public interface IntegrationRegistrar {

    /** Metadata describing this integration. */
    IntegrationDescriptor descriptor();

    /** Whether this integration is enabled in config. */
    boolean isEnabled();

    /** Registers adapters when enabled. */
    void registerEnabled();

    /** Registers fallback guards when disabled. */
    void registerDisabledGuards();

    /**
     * Registers an adapter with RetroPolymorph and associates it with this integration
     * in {@link IntegrationHealthRegistry} for precise diagnostics.
     */
    default void registerAdapter(ResourceLocation adapterId, int priority, RecipeSelectionAdapter adapter) {
        RetroPolymorphAPI.registerAdapter(adapterId, priority, adapter);
        IntegrationHealthRegistry.associateAdapter(descriptor().getId(), adapterId);
    }

    /**
     * Registers a machine adapter with RetroPolymorph and associates it with this integration
     * in {@link IntegrationHealthRegistry} for precise diagnostics.
     */
    default void registerMachineAdapter(ResourceLocation adapterId, int priority, dev.sosea1.retropolymorph.api.MachineRecipeAdapter adapter) {
        registerAdapter(adapterId, priority, new dev.sosea1.retropolymorph.machine.MachineRecipeSelectionAdapter(adapter));
    }
}
