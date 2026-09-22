package dev.sosea1.retropolymorph.compat.rsb;

import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Accessor for Retro Sophisticated Backpacks container topology and installed upgrades.
 */
public final class RsbContainerAccess {

    private RsbContainerAccess() {
    }

    public static boolean recognizes(Container container) {
        return container != null && RsbBindings.CONTAINER_CLASS.equals(container.getClass().getName());
    }

    @Nullable
    public static Object readBackpackWrapper(Container container) {
        return RsbBindings.getBackpackWrapper(container);
    }

    public static Set<Integer> findInstalledCraftingUpgradeIndices(Object backpackWrapper) {
        if (backpackWrapper == null) {
            return Collections.emptySet();
        }
        Object value = RsbBindings.invokeNoArg(backpackWrapper, "getUpgradeItemStackHandler");
        if (!(value instanceof IItemHandler)) {
            return Collections.emptySet();
        }

        IItemHandler upgrades = (IItemHandler) value;
        Set<Integer> indices = new LinkedHashSet<Integer>();
        int slots;
        try {
            slots = upgrades.getSlots();
        } catch (RuntimeException | LinkageError exception) {
            return indices;
        }
        for (int slot = 0; slot < slots; slot++) {
            try {
                ItemStack stack = upgrades.getStackInSlot(slot);
                if (isCraftingUpgradeStack(stack)) {
                    indices.add(Integer.valueOf(slot));
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return indices;
    }

    public static boolean isCraftingUpgradeStack(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return false;
        }
        String itemClass = stack.getItem().getClass().getName();
        if (itemClass.endsWith(".CraftingUpgradeItem") || itemClass.contains("CraftingUpgrade")) {
            return true;
        }
        if (stack.getItem().getRegistryName() == null) {
            return false;
        }
        String namespace = stack.getItem().getRegistryName().getNamespace();
        String path = stack.getItem().getRegistryName().getPath();
        return ("retro_sophisticated_backpacks".equals(namespace)
                || "retrosophisticatedbackpacks".equals(namespace))
                && path != null
                && path.toLowerCase(java.util.Locale.ROOT).contains("crafting");
    }

    public static boolean isCandidateTabOpened(@Nullable Object backpackWrapper, int upgradeIndex) {
        if (backpackWrapper == null) {
            return false;
        }
        Object value = RsbBindings.invokeNoArg(backpackWrapper, "getUpgradeItemStackHandler");
        if (!(value instanceof IItemHandler)) {
            return false;
        }
        IItemHandler upgrades = (IItemHandler) value;
        if (upgradeIndex < 0 || upgradeIndex >= upgrades.getSlots()) {
            return false;
        }
        try {
            ItemStack stack = upgrades.getStackInSlot(upgradeIndex);
            return RsbBindings.isUpgradeTabOpened(stack);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

}
