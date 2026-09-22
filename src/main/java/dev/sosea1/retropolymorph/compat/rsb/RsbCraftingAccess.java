package dev.sosea1.retropolymorph.compat.rsb;

import dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accessor for Retro Sophisticated Backpacks crafting matrices, result slots,
 * and client-side mirrors.
 */
public final class RsbCraftingAccess {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final String CRAFTING_SLOT_CLASS_SUFFIX = ".IndexedModularCraftingSlot";

    private static final Container CLIENT_MIRROR_OWNER = new ClientMirrorContainer();
    private static final Map<IItemHandler, InventoryCrafting> CLIENT_MIRRORS =
            Collections.synchronizedMap(new WeakHashMap<IItemHandler, InventoryCrafting>());
    private static final Set<String> LOGGED_SLOT_BRIDGES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> LOGGED_UNRESOLVED =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private RsbCraftingAccess() {
    }

    public static class ActiveCrafting {
        public final int upgradeIndex;
        public final InventoryCrafting matrix;
        public final Slot resultSlot;
        public final boolean slotFallback;
        @Nullable
        public final IItemHandler clientHandler;

        public ActiveCrafting(
                int upgradeIndex,
                InventoryCrafting matrix,
                Slot resultSlot,
                boolean slotFallback) {
            this(upgradeIndex, matrix, resultSlot, slotFallback, null);
        }

        public ActiveCrafting(
                int upgradeIndex,
                InventoryCrafting matrix,
                Slot resultSlot,
                boolean slotFallback,
                @Nullable IItemHandler clientHandler) {
            this.upgradeIndex = upgradeIndex;
            this.matrix = matrix;
            this.resultSlot = resultSlot;
            this.slotFallback = slotFallback;
            this.clientHandler = clientHandler;
        }

        public boolean isClientMirror() {
            return this.clientHandler != null;
        }
    }

    @Nullable
    public static ActiveCrafting resolveActiveCrafting(Container container) {
        if (!RsbContainerAccess.recognizes(container)) {
            return null;
        }

        try {
            Map<?, ?> matrices = RsbBindings.getMatrices(container);
            Map<?, ?> results = RsbBindings.getResults(container);

            Object backpackWrapper = RsbContainerAccess.readBackpackWrapper(container);
            Set<Integer> installedCrafting = backpackWrapper == null
                    ? Collections.<Integer>emptySet()
                    : RsbContainerAccess.findInstalledCraftingUpgradeIndices(backpackWrapper);

            List<ActiveCrafting> all = new ArrayList<ActiveCrafting>();

            // Preferred / server path: RSB's two keyed maps
            if (matrices != null && results != null) {
                for (Map.Entry<?, ?> entry : matrices.entrySet()) {
                    Object key = entry.getKey();
                    Object matrixValue = entry.getValue();
                    Object resultValue = results.get(key);
                    if (!(matrixValue instanceof InventoryCrafting) || !(resultValue instanceof Slot)) {
                        continue;
                    }

                    int upgradeIndex = key instanceof Number
                            ? ((Number) key).intValue()
                            : key == null ? 0 : key.hashCode();
                    if (!installedCrafting.isEmpty()
                            && !installedCrafting.contains(Integer.valueOf(upgradeIndex))) {
                        continue;
                    }
                    addCandidate(all, new ActiveCrafting(
                            upgradeIndex,
                            (InventoryCrafting) matrixValue,
                            (Slot) resultValue,
                            false));
                }

                // Client path A: result-slot map direct resolution
                for (Map.Entry<?, ?> entry : results.entrySet()) {
                    Object value = entry.getValue();
                    if (!(value instanceof Slot)) {
                        continue;
                    }
                    Slot slot = (Slot) value;
                    if (!isRsbCraftingSlot(slot)) {
                        continue;
                    }
                    int upgradeIndex = entry.getKey() instanceof Number
                            ? ((Number) entry.getKey()).intValue()
                            : readUpgradeIndex(slot);
                    if (!installedCrafting.isEmpty()
                            && !installedCrafting.contains(Integer.valueOf(upgradeIndex))) {
                        continue;
                    }
                    InventoryCrafting matrix = matrixFromMap(matrices, upgradeIndex);
                    if (matrix == null) {
                        matrix = readCraftMatrix(slot);
                    }
                    if (matrix != null) {
                        addCandidate(all, new ActiveCrafting(upgradeIndex, matrix, slot, true));
                    }
                }
            }

            // Client path B: container inventorySlots fallback
            for (Slot slot : container.inventorySlots) {
                if (!isRsbCraftingSlot(slot)) {
                    continue;
                }
                int upgradeIndex = readUpgradeIndex(slot);
                if (!installedCrafting.isEmpty()
                        && !installedCrafting.contains(Integer.valueOf(upgradeIndex))) {
                    continue;
                }
                InventoryCrafting matrix = matrices != null ? matrixFromMap(matrices, upgradeIndex) : null;
                if (matrix == null) {
                    matrix = readCraftMatrix(slot);
                }
                if (matrix == null) {
                    continue;
                }
                addCandidate(all, new ActiveCrafting(upgradeIndex, matrix, slot, true));
            }

            // Client path C: ModularUI client mirror via capability provider on upgrade item
            if (all.isEmpty() && backpackWrapper != null) {
                all.addAll(resolveClientCrafting(container, results));
            }

            ActiveCrafting active = selectActive(all, backpackWrapper);
            if (active == null) {
                int outputSlots = 0;
                for (Slot slot : container.inventorySlots) {
                    if (isRsbCraftingSlot(slot)) {
                        outputSlots++;
                    }
                }
                String summary = "maps=" + (matrices != null ? matrices.size() : -1)
                        + ", resultMaps=" + (results != null ? results.size() : -1)
                        + ", craftingOutputSlots=" + outputSlots
                        + ", candidates=" + all.size();
                if (LOGGED_UNRESOLVED.add(summary)) {
                    LOGGER.debug("RSB crafting bridge unresolved: {}", summary);
                }
            }

            if (active != null && active.slotFallback) {
                String key = container.getClass().getName() + "|" + active.upgradeIndex
                        + "|" + (active.clientHandler == null ? "slot" : "client-wrapper");
                if (LOGGED_SLOT_BRIDGES.add(key)) {
                    if (active.clientHandler != null) {
                        LOGGER.debug(
                                "RSB client crafting-wrapper bridge active: container={}, upgrade={}, matrix={}, resultSlot={}",
                                container.getClass().getName(),
                                Integer.valueOf(active.upgradeIndex),
                                active.matrix.getClass().getName(),
                                active.resultSlot.getClass().getName());
                    } else {
                        LOGGER.debug(
                                "RSB crafting slot bridge active: container={}, upgrade={}, matrix={}, resultSlot={}",
                                container.getClass().getName(),
                                Integer.valueOf(active.upgradeIndex),
                                active.matrix.getClass().getName(),
                                active.resultSlot.getClass().getName());
                    }
                }
            }
            return active;
        } catch (RuntimeException | LinkageError error) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "retrosophisticatedbackpacks",
                    "resolveActiveCrafting",
                    error);
            return null;
        }
    }

    private static List<ActiveCrafting> resolveClientCrafting(
            Container container,
            @Nullable Map<?, ?> results) {
        Object backpackWrapper = RsbContainerAccess.readBackpackWrapper(container);
        if (backpackWrapper == null) {
            return Collections.emptyList();
        }

        Set<Integer> installedCrafting = RsbContainerAccess.findInstalledCraftingUpgradeIndices(backpackWrapper);
        if (installedCrafting.isEmpty()) {
            return Collections.emptyList();
        }

        Object upgradeHandler = RsbBindings.invokeNoArg(backpackWrapper, "getUpgradeItemStackHandler");
        if (!(upgradeHandler instanceof IItemHandler)) {
            return Collections.emptyList();
        }

        IItemHandler upgrades = (IItemHandler) upgradeHandler;
        List<ActiveCrafting> resolved = new ArrayList<ActiveCrafting>();

        for (Integer idx : installedCrafting) {
            int slot = idx.intValue();
            if (slot < 0 || slot >= upgrades.getSlots()) {
                continue;
            }
            ItemStack stack = upgrades.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            IItemHandler matrixHandler = RsbBindings.extractCraftMatrixFromStack(stack);
            if (matrixHandler == null && stack.hasTagCompound() && stack.getTagCompound().hasKey("Matrix", 10)) {
                matrixHandler = RsbBindings.deserializeExposedHandler(stack.getTagCompound().getCompoundTag("Matrix"), 9);
            }
            if (matrixHandler == null) {
                continue;
            }

            Slot resultSlot = results != null ? resultSlotFromMap(results, slot) : null;
            if (resultSlot == null) {
                for (Slot s : container.inventorySlots) {
                    if (isRsbCraftingSlot(s) && readUpgradeIndex(s) == slot) {
                        resultSlot = s;
                        break;
                    }
                }
            }
            if (resultSlot == null) {
                continue;
            }

            InventoryCrafting mirror = CLIENT_MIRRORS.get(matrixHandler);
            if (mirror == null) {
                mirror = new InventoryCrafting(CLIENT_MIRROR_OWNER, 3, 3);
                CLIENT_MIRRORS.put(matrixHandler, mirror);
            }
            syncClientMatrix(matrixHandler, mirror);
            addCandidate(resolved, new ActiveCrafting(slot, mirror, resultSlot, true, matrixHandler));
        }

        return resolved;
    }

    @Nullable
    private static ActiveCrafting selectActive(List<ActiveCrafting> all, @Nullable Object backpackWrapper) {
        if (all.isEmpty()) {
            return null;
        }

        if (all.size() == 1) {
            return all.get(0);
        }

        // 1. If multiple candidates exist, prefer the one with its upgrade tab opened
        if (backpackWrapper != null) {
            List<ActiveCrafting> opened = new ArrayList<ActiveCrafting>();
            for (ActiveCrafting candidate : all) {
                if (RsbContainerAccess.isCandidateTabOpened(backpackWrapper, candidate.upgradeIndex)) {
                    opened.add(candidate);
                }
            }
            if (opened.size() == 1) {
                return opened.get(0);
            }
            if (!opened.isEmpty()) {
                all = opened;
            }
        }

        // 2. Check enabled result slots
        List<ActiveCrafting> enabled = new ArrayList<ActiveCrafting>();
        for (ActiveCrafting candidate : all) {
            if (isEnabled(candidate.resultSlot)) {
                enabled.add(candidate);
            }
        }

        List<ActiveCrafting> enabledWithInputs = withInputs(enabled);
        if (enabledWithInputs.size() == 1) {
            return enabledWithInputs.get(0);
        }
        if (enabled.size() == 1) {
            return enabled.get(0);
        }

        List<ActiveCrafting> allWithInputs = withInputs(all);
        if (allWithInputs.size() == 1) {
            return allWithInputs.get(0);
        }
        if (enabled.isEmpty() && all.size() == 1) {
            return all.get(0);
        }

        // 3. Fallback to first installed candidate
        return all.isEmpty() ? null : all.get(0);
    }

    private static List<ActiveCrafting> withInputs(List<ActiveCrafting> candidates) {
        List<ActiveCrafting> result = new ArrayList<ActiveCrafting>();
        for (ActiveCrafting candidate : candidates) {
            if (hasInputs(candidate.matrix)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private static boolean hasInputs(InventoryCrafting matrix) {
        int max = Math.min(matrix.getSizeInventory(), matrix.getWidth() * matrix.getHeight());
        for (int index = 0; index < max; index++) {
            if (!matrix.getStackInSlot(index).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnabled(Slot slot) {
        return slot != null && RsbBindings.getSlotBinding(slot.getClass()).isEnabled(slot);
    }

    private static void addCandidate(List<ActiveCrafting> candidates, ActiveCrafting candidate) {
        for (ActiveCrafting existing : candidates) {
            if (existing.matrix == candidate.matrix && existing.resultSlot == candidate.resultSlot) {
                return;
            }
            if (existing.upgradeIndex == candidate.upgradeIndex
                    && existing.resultSlot == candidate.resultSlot) {
                return;
            }
        }
        candidates.add(candidate);
    }

    public static boolean isRsbCraftingSlot(@Nullable Slot slot) {
        return slot != null && slot.getClass().getName().endsWith(CRAFTING_SLOT_CLASS_SUFFIX);
    }

    public static int readUpgradeIndex(Slot slot) {
        if (slot == null) {
            return 0;
        }
        return RsbBindings.getSlotBinding(slot.getClass()).readUpgradeIndex(slot);
    }

    @Nullable
    public static InventoryCrafting readCraftMatrix(Slot slot) {
        if (slot == null) {
            return null;
        }
        if (slot.inventory instanceof InventoryCrafting) {
            return (InventoryCrafting) slot.inventory;
        }
        return RsbBindings.getSlotBinding(slot.getClass()).readCraftMatrix(slot);
    }

    @Nullable
    public static InventoryCrafting matrixFromMap(Map<?, ?> matrices, int upgradeIndex) {
        Object direct = matrices.get(Integer.valueOf(upgradeIndex));
        if (direct instanceof InventoryCrafting) {
            return (InventoryCrafting) direct;
        }
        for (Map.Entry<?, ?> entry : matrices.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Number
                    && ((Number) key).intValue() == upgradeIndex
                    && entry.getValue() instanceof InventoryCrafting) {
                return (InventoryCrafting) entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    public static Slot resultSlotFromMap(Map<?, ?> results, int upgradeIndex) {
        Object direct = results.get(Integer.valueOf(upgradeIndex));
        if (direct instanceof Slot) {
            return (Slot) direct;
        }
        for (Map.Entry<?, ?> entry : results.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Number
                    && ((Number) key).intValue() == upgradeIndex
                    && entry.getValue() instanceof Slot) {
                return (Slot) entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    public static IItemHandler extractMatrixHandler(@Nullable ActiveCrafting active) {
        if (active == null) {
            return null;
        }
        if (active.clientHandler != null) {
            return active.clientHandler;
        }
        Object delegate = RsbBindings.invokeNoArg(active.matrix, "getDelegate");
        if (delegate instanceof IItemHandler) {
            return (IItemHandler) delegate;
        }
        return null;
    }

    public static void refreshClientMatrix(@Nullable ActiveCrafting active) {
        if (active != null && active.clientHandler != null) {
            syncClientMatrix(active.clientHandler, active.matrix);
        }
    }

    private static void syncClientMatrix(IItemHandler handler, InventoryCrafting mirror) {
        int sourceSlots;
        try {
            sourceSlots = Math.min(9, handler.getSlots());
        } catch (RuntimeException | LinkageError exception) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack source = ItemStack.EMPTY;
            if (slot < sourceSlots) {
                try {
                    ItemStack stack = handler.getStackInSlot(slot);
                    source = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
                } catch (RuntimeException | LinkageError ignored) {
                    source = ItemStack.EMPTY;
                }
            }
            ItemStack current = mirror.getStackInSlot(slot);
            if (current != source && !ItemStack.areItemStacksEqual(current, source)) {
                mirror.setInventorySlotContents(slot, source);
            }
        }
    }

    private static final class ClientMirrorContainer extends Container {

        @Override
        public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) {
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }
}
