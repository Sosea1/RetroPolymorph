package dev.sosea1.retropolymorph.api;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Opt-in recipe-selection contract for machines/processors that are not ordinary
 * {@code InventoryCrafting} or vanilla-furnace surfaces.
 *
 * <p>The owning integration stays authoritative for recipe matching, native
 * preview refresh and the actual operation that consumes inputs. RetroPolymorph
 * only handles conflict transport, selector UX and preference policy.</p>
 *
 * <p>For fluid/energy/mode inputs that cannot be represented as ItemStacks,
 * include their cheap client-visible fingerprint in {@link #getClientStateToken()}.
 * That token participates in selector cache invalidation.</p>
 */
public interface MachineRecipeSurface {

    /** The currently open container associated with this surface. */
    Container getContainer();

    /**
     * Stable recipe owner identity, usually a TileEntity or another persistent
     * machine object. Exposed for diagnostics/integration bookkeeping only.
     */
    Object getRecipeOwner();

    /** Number of ItemStack inputs participating in the client snapshot. */
    int getInputCount();

    /** One ItemStack input, or EMPTY for an out-of-range index. */
    ItemStack getInputStack(int index);

    /**
     * Cheap state hash for non-ItemStack recipe inputs/modes. Implementations
     * should keep this deterministic and allocation-free on the GUI hot path.
     */
    default int getClientStateToken() {
        return 0;
    }

    /** All currently selectable conflicting options in native engine order. */
    List<RecipeOption> findOptions(World world);

    /**
     * Applies one authoritative selection to the owning machine. Implementations
     * must revalidate the key against current inputs before returning true.
     */
    boolean selectRecipe(String recipeKey, World world);

    /** Clears the machine/session selection. */
    void clearRecipeSelection();

    /** Current stable key, or null when the machine follows its native default. */
    @Nullable
    String getSelectedRecipeKey();

    /** Optional client-side mirror of the authoritative server choice. */
    default void applyRemoteSelection(@Nullable String recipeKey) {
    }

    /**
     * Refreshes the native preview/cache after a server-side select/clear.
     * Kept separate from state mutation so integrations have one predictable
     * lifecycle hook.
     */
    default void refreshPreview() {
    }

    /**
     * Who owns/persists the selection. Shared machines should normally use
     * OWNER, or OWNER_WITH_PLAYER_PROFILE when a durable machine choice should
     * also follow the player's global conflict preference on GUI queries.
     */
    default MachineRecipePersistence getPersistencePolicy() {
        return MachineRecipePersistence.OWNER;
    }

    /** Dynamic empty-input retention policy, matching SelectionContext semantics. */
    default boolean retainSelectionWhenOptionsEmpty() {
        return false;
    }

    /**
     * Safety gate: true only when the integration also controls/seeds the exact
     * native lookup used by the real machine operation. A cosmetic-only preview
     * integration is rejected by the bridge and generic fallback stays blocked.
     */
    default boolean controlsActualOperation() {
        return false;
    }

    /** Optional real result slot. Virtual-preview machines may return null. */
    @Nullable
    Slot getResultSlot();

    default SelectorPlacement getSelectorPlacement() {
        Slot result = getResultSlot();
        return result != null
                ? SelectorPlacement.resultSlot(result.xPos, result.yPos, 0, 0)
                : SelectorPlacement.hidden();
    }

}

