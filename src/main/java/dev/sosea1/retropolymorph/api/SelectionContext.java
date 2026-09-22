package dev.sosea1.retropolymorph.api;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Minimal common contract for one user-facing recipe-selection surface.
 *
 * Implementations expose only the input stacks needed for client-side cache
 * invalidation. The recipe engine itself remains private to the context.
 */
public interface SelectionContext {

    Container getContainer();

    @Nullable
    Slot getResultSlot();

    int getInputCount();

    ItemStack getInputStack(int index);

    /**
     * Cheap client-visible state that affects recipe availability without
     * necessarily changing the input stacks (for example a terminal mode).
     * Returning a different value invalidates the authoritative option snapshot.
     */
    default int getClientStateToken() {
        return 0;
    }

    List<RecipeOption> findOptions(World world);

    boolean select(String recipeKey, World world);

    void clearSelection();

    @Nullable
    String getSelectedRecipeKey();

    /**
     * Whether the client should retain the authoritative selection while this
     * context temporarily exposes no choices. This is deliberately dynamic: a
     * furnace keeps its selection only while the input slot is actually empty,
     * not when a different non-conflicting input replaces the old one.
     */
    default boolean retainSelectionWhenOptionsEmpty() {
        return false;
    }

    /**
     * Applies an authoritative server selection to optional client-side state.
     * Most contexts do not cache selection locally and keep the default no-op.
     */
    default void applyRemoteSelection(@Nullable String recipeKey) {
    }

    default SelectionPersistencePolicy getPersistencePolicy() {
        return SelectionPersistencePolicy.PLAYER_PERSISTENT;
    }

    default SelectorPlacement getSelectorPlacement() {
        Slot result = getResultSlot();
        return result != null
                ? SelectorPlacement.resultSlot(result.xPos, result.yPos, 0, 0)
                : SelectorPlacement.hidden();
    }

    default SelectionScope getSelectionScope() {
        return SelectionScope.local();
    }
}

