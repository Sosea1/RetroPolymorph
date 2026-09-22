package dev.sosea1.retropolymorph.compat.ae2;

import appeng.helpers.InventoryAction;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens the same recipe-selection execution scope used by native AE2 when an
 * AE2WTLib/WCT-style container handles its own InventoryAction packet.
 *
 * Those addons use a custom crafting-result slot and bypass
 * SlotCraftingTerm#doClick entirely. This helper brackets
 * the addon's real doAction call, pins the selected output before it can be
 * snapshotted, and keeps the selected recipe visible to any scratch 3x3
 * InventoryCrafting created during the action.
 */
public final class Ae2AddonCraftActionScope {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final Set<String> LOGGED_ACTIONS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final ThreadLocal<Deque<Boolean>> ENTERED =
            new ThreadLocal<Deque<Boolean>>() {
                @Override
                protected Deque<Boolean> initialValue() {
                    return new ArrayDeque<Boolean>();
                }
            };

    private Ae2AddonCraftActionScope() {
    }

    public static void begin(
            Container container,
            EntityPlayerMP player,
            InventoryAction action,
            int slotIndex) {
        boolean entered = false;

        if (isCraftAction(action)
                && container != null
                && player != null
                && player.openContainer == container
                && isCraftingResultSlot(container, slotIndex)) {
            Ae2CraftExecutionScope.enter(player);
            ResourceLocation selected = Ae2CraftExecutionScope.currentSelectedRecipeId();
            if (selected != null) {
                // Critical ordering: WCT snapshots/uses its custom result slot
                // during its own doAction path. Put the selected output there
                // before the addon executes any of that code.
                Ae2TerminalRecipePin.pinContainerResult(container, "addonDoAction");
                String logKey = container.getClass().getName() + "|" + action + "|" + selected;
                if (LOGGED_ACTIONS.add(logKey)) {
                    LOGGER.debug(
                            "AE2 addon crafting execution scope active: container={}, window={}, action={}, slot={}, selected={}",
                            container.getClass().getName(),
                            Integer.valueOf(container.windowId),
                            action,
                            Integer.valueOf(slotIndex),
                            selected);
                }
                entered = true;
            } else {
                Ae2CraftExecutionScope.exit();
            }
        }

        ENTERED.get().push(Boolean.valueOf(entered));
    }

    public static void end() {
        Deque<Boolean> stack = ENTERED.get();
        if (stack.isEmpty()) {
            return;
        }
        boolean entered = stack.pop().booleanValue();
        if (entered) {
            Ae2CraftExecutionScope.exit();
        }
        if (stack.isEmpty()) {
            ENTERED.remove();
        }
    }

    private static boolean isCraftAction(InventoryAction action) {
        return action == InventoryAction.CRAFT_ITEM
                || action == InventoryAction.CRAFT_STACK
                || action == InventoryAction.CRAFT_SHIFT;
    }

    private static boolean isCraftingResultSlot(Container container, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= container.inventorySlots.size()) {
            return false;
        }
        Slot slot = container.getSlot(slotIndex);
        return slot instanceof Ae2CraftingResultSlot;
    }
}
