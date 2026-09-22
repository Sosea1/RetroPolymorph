package dev.sosea1.retropolymorph.compat.ae2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Narrow server-thread scope for native AE2 terminal craft execution.
 *
 * <p>AE2's SlotCraftingTerm validates the click against a fresh
 * InventoryCrafting(new ContainerNull(), 3, 3). That scratch matrix has no
 * terminal owner, so this scope carries the recipe selected for exactly the
 * player's active doClick call. It is stack-shaped for nested calls and stores
 * the originating player/container metadata for diagnostics and isolation.</p>
 */
public final class Ae2CraftExecutionScope {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<Frame>();
    private static final Set<String> LOGGED_CONTAINERS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private Ae2CraftExecutionScope() {
    }

    public static void enter(EntityPlayer player) {
        Frame previous = CURRENT.get();
        ResourceLocation selected = null;
        Container container = null;
        UUID playerId = null;
        String containerClass = "<none>";

        if (player != null) {
            playerId = player.getUniqueID();
            container = player.openContainer;
            if (container != null) {
                containerClass = container.getClass().getName();
                selected = Ae2SelectionStore.get(container);
                if (selected == null && container instanceof Ae2CraftingTermExtension) {
                    selected = ((Ae2CraftingTermExtension) container)
                            .retropolymorph$getAe2SelectedRecipeId();
                    if (selected != null) {
                        Ae2SelectionStore.set(container, selected);
                    }
                }
            }
        }

        CURRENT.set(new Frame(selected, playerId, container, previous));
        if (selected != null && LOGGED_CONTAINERS.add(containerClass)) {
            LOGGER.debug(
                    "AE2 crafting execution scope active: player={}, container={}, window={}, selected={}",
                    playerId,
                    containerClass,
                    Integer.valueOf(container == null ? -1 : container.windowId),
                    selected);
        }
    }

    public static void exit() {
        Frame current = CURRENT.get();
        if (current == null || current.previous == null) {
            CURRENT.remove();
            return;
        }
        CURRENT.set(current.previous);
    }

    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    @Nullable
    public static ResourceLocation currentSelectedRecipeId() {
        Frame current = CURRENT.get();
        return current == null ? null : current.selectedRecipeId;
    }

    @Nullable
    public static Container currentContainer() {
        Frame current = CURRENT.get();
        return current == null ? null : current.container;
    }

    /**
     * Captures the result that was pinned at the start of the current craft.
     * Some wireless-terminal implementations recompute their visible slot
     * correctly but still return an older result stack from the inner craft
     * routine. Keeping the expected output in this already player/container-
     * scoped frame lets a final return-value guard repair only that one craft.
     */
    public static void setExpectedOutput(@Nullable ItemStack output) {
        Frame current = CURRENT.get();
        if (current == null) {
            return;
        }
        current.expectedOutput = output == null || output.isEmpty()
                ? ItemStack.EMPTY
                : output.copy();
    }

    public static ItemStack currentExpectedOutput() {
        Frame current = CURRENT.get();
        if (current == null || current.expectedOutput == null || current.expectedOutput.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return current.expectedOutput.copy();
    }

    private static final class Frame {
        @Nullable
        private final ResourceLocation selectedRecipeId;
        @Nullable
        private final UUID playerId;
        @Nullable
        private final Container container;
        @Nullable
        private final Frame previous;
        private ItemStack expectedOutput = ItemStack.EMPTY;

        private Frame(
                @Nullable ResourceLocation selectedRecipeId,
                @Nullable UUID playerId,
                @Nullable Container container,
                @Nullable Frame previous) {
            this.selectedRecipeId = selectedRecipeId;
            this.playerId = playerId;
            this.container = container;
            this.previous = previous;
        }
    }
}
