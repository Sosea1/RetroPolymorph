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

    Slot getResultSlot();

    int getInputCount();

    ItemStack getInputStack(int index);

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

    default int getButtonOffsetX() {
        return 0;
    }

    default int getButtonOffsetY() {
        return 0;
    }
}
