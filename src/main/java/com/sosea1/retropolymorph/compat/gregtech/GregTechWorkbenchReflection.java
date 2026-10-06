package com.sosea1.retropolymorph.compat.gregtech;

import com.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import com.sosea1.retropolymorph.core.CraftingMatrixExtension;
import com.sosea1.retropolymorph.core.RecipeSelectionState;
import com.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import com.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class GregTechWorkbenchReflection {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final Set<String> REPORTED_REFRESH_FAILURES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static final String GTCE_CONTAINER =
            "gregtech.api.gui.impl.ModularUIContainer";
    private static final String GTCE_SLOT_WIDGET =
            "gregtech.common.gui.widget.CraftingSlotWidget";
    private static final String GTCE_RESOLVER =
            "gregtech.common.metatileentities.storage.CraftingRecipeResolver";

    private static final String GTCEU_CONTAINER =
            "com.cleanroommc.modularui.screen.ModularContainer";
    private static final String GTCEU_LOGIC =
            "gregtech.common.metatileentities.storage.CraftingRecipeLogic";
    private static final String GTCEU_LEGACY_SLOT_WIDGET =
            "gregtech.common.gui.widget.craftingstation.CraftingSlotWidget";

    // The template is separate from GT's mutable execution grid. Stable wrappers
    // preserve selection state across probes on both sides.
    private static final Map<Container, InventoryCrafting> TEMPLATE_MATRICES =
            new WeakHashMap<Container, InventoryCrafting>();

    private GregTechWorkbenchReflection() {
    }

    static boolean isSupportedContainer(@Nullable Container container) {
        if (container == null) {
            return false;
        }
        Class<?> type = container.getClass();
        return hasClassInHierarchy(type, GTCE_CONTAINER)
                || hasClassInHierarchy(type, GTCEU_CONTAINER);
    }

    @Nullable
    static Binding bind(Container container) {
        try {
            if (hasClassInHierarchy(container.getClass(), GTCEU_CONTAINER)) {
                return bindCeu(container);
            }
            if (hasClassInHierarchy(container.getClass(), GTCE_CONTAINER)) {
                return bindCe(container);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "gregtech", "bindWorkbench", exception);
        }
        return null;
    }

    static boolean refresh(Binding binding) {
        return refresh(binding, null);
    }

    static boolean refresh(Binding binding, @Nullable IRecipe selectedRecipe) {
        try {
            if (binding.engine == null) {
                // Null means native/automatic selection, not an empty recipe.
                // Only GT's server knows ingredient availability and the native output.
                if (selectedRecipe != null) {
                    ItemStack output = RecipeProbe.craftingResult(selectedRecipe, binding.matrix);
                    if (output.isEmpty()) {
                        return false;
                    }
                    binding.resultSlot.inventory.setInventorySlotContents(0,
                            output);
                }
            } else if (binding.ceu) {
                refreshCeu(binding, selectedRecipe);
            } else {
                refreshCe(binding, selectedRecipe);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "gregtech", "refreshWorkbench", exception);
            String profile = binding.engine == null ? "template" : binding.engine.getClass().getName();
            if (REPORTED_REFRESH_FAILURES.add(profile + "|" + exception.getClass().getName())) {
                LOGGER.warn("GregTech workbench selection could not be applied (profile={})", profile, exception);
            }
            return false;
        }
    }

    @Nullable
    private static Binding bindCeu(Container container)
            throws ReflectiveOperationException {
        Object syncManager = null;
        Field syncManagerField = findField(container.getClass(), "syncManager");
        if (syncManagerField != null) {
            syncManager = syncManagerField.get(container);
        }
        if (syncManager == null) {
            try {
                syncManager = invoke(container, "getSyncManager");
            } catch (Throwable ignored) {
            }
        }
        if (syncManager == null) {
            return null;
        }

        Object logic = findCeuLogic(syncManager);
        if (logic == null) {
            return null;
        }

        Object matrixValue = invoke(logic, "getCraftingMatrix");
        Object resultValue = invoke(logic, "getCraftingResultInventory");
        if (!(matrixValue instanceof InventoryCrafting)
                || !(resultValue instanceof IInventory)) {
            return null;
        }

        InventoryCrafting matrix = (InventoryCrafting) matrixValue;
        IInventory resultInventory = (IInventory) resultValue;
        return new Binding(
                container,
                logic,
                matrix,
                findResultSlot(container, resultInventory, 80, 47),
                true);
    }

    @Nullable
    private static Object findCeuLogic(Object syncManager) {
        try {
            Object mainPSM = invoke(syncManager, "getMainPSM");
            Object logic = findLogicInPanel(mainPSM);
            if (logic != null) {
                return logic;
            }

            Field panelMapField = findField(syncManager.getClass(), "panelSyncManagerMap");
            if (panelMapField != null) {
                Object panelMap = panelMapField.get(syncManager);
                if (panelMap instanceof java.util.Map) {
                    for (Object panel : ((java.util.Map<?, ?>) panelMap).values()) {
                        logic = findLogicInPanel(panel);
                        if (logic != null) {
                            return logic;
                        }
                    }
                }
            }

            String[] candidatePanels = new String[] { "workbench", "crafting_station", "craftingstation", "main" };
            for (String panelName : candidatePanels) {
                Object panel = invoke(syncManager, "getPanelSyncManager", String.class, panelName);
                logic = findLogicInPanel(panel);
                if (logic != null) {
                    return logic;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return null;
    }

    @Nullable
    private static Object findLogicInPanel(@Nullable Object panel) {
        if (panel == null) {
            return null;
        }
        try {
            Field syncHandlersField = findField(panel.getClass(), "syncHandlers");
            if (syncHandlersField != null) {
                Object handlersMap = syncHandlersField.get(panel);
                if (handlersMap instanceof java.util.Map) {
                    for (Object handler : ((java.util.Map<?, ?>) handlersMap).values()) {
                        if (handler != null && isCeuLogic(handler)) {
                            return handler;
                        }
                    }
                }
            }

            String[] candidateKeys = new String[] { "recipe_logic:0", "recipe_logic", "logic:0", "crafting_logic:0" };
            for (String key : candidateKeys) {
                Object handler = invoke(panel, "getSyncHandlerFromMapKey", String.class, key);
                if (handler == null) {
                    handler = invoke(panel, "getSyncHandler", String.class, key);
                }
                if (handler != null && isCeuLogic(handler)) {
                    return handler;
                }
            }

            Method findSyncHandlerNullable = findMethod(
                    panel.getClass(), "findSyncHandlerNullable", String.class, int.class);
            if (findSyncHandlerNullable != null) {
                Object logic = findSyncHandlerNullable.invoke(panel, "recipe_logic", Integer.valueOf(0));
                if (logic != null && isCeuLogic(logic)) {
                    return logic;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return null;
    }

    private static boolean isCeuLogic(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        if (GTCEU_LOGIC.equals(clazz.getName()) || hasClassInHierarchy(clazz, GTCEU_LOGIC)) {
            return true;
        }
        return findMethod(clazz, "getCraftingMatrix") != null
                && findMethod(clazz, "getCraftingResultInventory") != null;
    }

    @Nullable
    private static Binding bindCe(Container container)
            throws ReflectiveOperationException {
        Object modularUi = invoke(container, "getModularUI");
        Object visible = modularUi == null
                ? null
                : invoke(modularUi, "getFlatVisibleWidgetCollection");
        if (!(visible instanceof Collection)) {
            return null;
        }

        Object resolver = null;
        for (Object widget : (Collection<?>) visible) {
            if (widget != null && hasClassInHierarchy(widget.getClass(), GTCEU_LEGACY_SLOT_WIDGET)) {
                return bindLegacyCeu(container, modularUi, widget);
            }
            if (widget != null && GTCE_SLOT_WIDGET.equals(widget.getClass().getName())) {
                resolver = readField(widget, "recipeResolver");
                if (resolver == null) {
                    Object slot = readField(widget, "slotReference");
                    Object grid = clientTemplateGrid(modularUi);
                    if (!(slot instanceof Slot) || !(grid instanceof IItemHandler)
                            || ((IItemHandler) grid).getSlots() != 9) {
                        return null;
                    }
                    return new Binding(container, null, templateMatrix(container, (IItemHandler) grid),
                            (Slot) slot, false);
                }
                break;
            }
        }
        if (resolver == null || !GTCE_RESOLVER.equals(resolver.getClass().getName())) {
            return null;
        }

        Object matrixValue = readField(resolver, "inventoryCrafting");
        Object resultValue = invoke(resolver, "getCraftingResultInventory");
        if (!(matrixValue instanceof InventoryCrafting)
                || !(resultValue instanceof IInventory)) {
            return null;
        }

        InventoryCrafting matrix = (InventoryCrafting) matrixValue;
        IInventory resultInventory = (IInventory) resultValue;
        Object grid = readField(resolver, "craftingGrid");
        InventoryCrafting template = grid instanceof IItemHandler && ((IItemHandler) grid).getSlots() == 9
                ? templateMatrix(container, (IItemHandler) grid) : matrix;
        return new Binding(
                container,
                resolver,
                template,
                findResultSlot(container, resultInventory, 79, 35),
                false, matrix);
    }

    @Nullable
    private static Object clientTemplateGrid(Object modularUi) throws ReflectiveOperationException {
        Object holder = readField(modularUi, "holder");
        Object workbench = holder == null ? null : invoke(holder, "getMetaTileEntity");
        if (workbench == null) {
            return null;
        }
        Object grid = invoke(workbench, "getCraftingGrid");
        return grid != null ? grid : readField(workbench, "craftingGrid");
    }

    @Nullable
    private static Binding bindLegacyCeu(Container container, Object modularUi, Object widget)
            throws ReflectiveOperationException {
        Object logic = readField(widget, "recipeResolver");
        Object slotValue = readField(widget, "slotReference");
        if (!(slotValue instanceof Slot)) {
            return null;
        }
        Slot resultSlot = (Slot) slotValue;
        if (logic != null) {
            if (!hasClassInHierarchy(logic.getClass(), GTCEU_LOGIC)) {
                return null;
            }
            Object matrix = readField(logic, "inventoryCrafting");
            if (!(matrix instanceof InventoryCrafting)) {
                return null;
            }
            Object grid = readField(logic, "craftingGrid");
            InventoryCrafting template = grid instanceof IItemHandler && ((IItemHandler) grid).getSlots() == 9
                    ? templateMatrix(container, (IItemHandler) grid) : (InventoryCrafting) matrix;
            return new Binding(container, logic, template, resultSlot, true, (InventoryCrafting) matrix);
        }

        // Released GTCEu creates CraftingRecipeLogic on the server only; its
        // client matrix lives in the tile's synchronized ghost crafting grid.
        Object holder = readField(modularUi, "holder");
        Object workbench = holder == null ? null : invoke(holder, "getMetaTileEntity");
        Object grid = workbench == null ? null : invoke(workbench, "getCraftingGrid");
        if (!(grid instanceof IItemHandler) || ((IItemHandler) grid).getSlots() != 9) {
            return null;
        }
        InventoryCrafting matrix = templateMatrix(container, (IItemHandler) grid);
        return new Binding(container, null, matrix, resultSlot, true);
    }

    private static InventoryCrafting templateMatrix(Container container, IItemHandler grid) {
        InventoryCrafting matrix;
        synchronized (TEMPLATE_MATRICES) {
            matrix = TEMPLATE_MATRICES.get(container);
            if (matrix == null) {
                matrix = new WorkbenchTemplateMatrix(grid);
                TEMPLATE_MATRICES.put(container, matrix);
            }
        }
        return matrix;
    }

    private static final class WorkbenchTemplateMatrix extends InventoryCrafting implements CraftingMatrixExtension {
        private final IItemHandler grid;
        private RecipeSelectionState selection;

        private WorkbenchTemplateMatrix(IItemHandler grid) {
            // Do not retain the UI container through a weak map value.
            super(null, 3, 3);
            this.grid = grid;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return index < 0 || index >= 9 ? ItemStack.EMPTY : this.grid.getStackInSlot(index);
        }

        @Override
        public ItemStack getStackInRowAndColumn(int row, int column) {
            return row < 0 || row >= 3 || column < 0 || column >= 3
                    ? ItemStack.EMPTY : getStackInSlot(row + column * 3);
        }

        @Override
        public boolean isEmpty() {
            for (int index = 0; index < 9; index++) {
                if (!getStackInSlot(index).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public RecipeSelectionState retropolymorph$peekRecipeSelectionState() {
            return this.selection;
        }

        @Override
        public RecipeSelectionState retropolymorph$getOrCreateRecipeSelectionState() {
            if (this.selection == null) {
                this.selection = new RecipeSelectionState();
            }
            return this.selection;
        }

        @Override
        public Container retropolymorph$getCraftingOwner() {
            return null;
        }
    }

    @Nullable
    static World world(Binding binding) {
        try {
            Object value = binding.engine == null ? null : readField(binding.engine, "world");
            return value instanceof World ? (World) value : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    static ItemStack memoryResult(Object widget) throws ReflectiveOperationException {
        Object memory = readField(widget, "recipeMemory");
        Object index = readField(widget, "recipeIndex");
        if (memory == null || !(index instanceof Integer)) {
            return ItemStack.EMPTY;
        }
        Object recipe = invoke(memory, "getRecipeAtIndex", int.class, index);
        Object result = recipe == null ? null : invoke(recipe, "getRecipeResult");
        return result instanceof ItemStack ? ((ItemStack) result).copy() : ItemStack.EMPTY;
    }

    private static Slot findResultSlot(
            Container container,
            IInventory resultInventory,
            int fallbackX,
            int fallbackY) {
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == resultInventory) {
                return slot;
            }
        }
        return new Slot(resultInventory, 0, fallbackX, fallbackY);
    }

    private static void refreshCeu(Binding binding, @Nullable IRecipe selectedRecipe)
            throws ReflectiveOperationException {
        if (selectedRecipe != null) {
            applyCeuSelection(binding, selectedRecipe);
            return;
        }
        Object logic = binding.engine;
        // GT updates its execution grid lazily, after ghost-slot packets. A selector
        // request can arrive first; let GT copy the new template before pinning it.
        Method synchronize = findMethod(logic.getClass(), "hasCraftingGridUpdated");
        if (synchronize != null) {
            synchronize.invoke(logic);
        }
        // Native ingredient substitution may retain a valid alternative from the
        // previous recipe. Start a new choice from the visible template; GT still
        // checks availability and consumes through its own CachedRecipeData.
        if (binding.matrix != binding.executionMatrix) {
            for (int slot = 0; slot < binding.matrix.getSizeInventory(); slot++) {
                ItemStack template = binding.matrix.getStackInSlot(slot);
                if (!ItemStack.areItemStacksEqual(template, binding.executionMatrix.getStackInSlot(slot))) {
                    binding.executionMatrix.setInventorySlotContents(slot, template.copy());
                }
            }
        }
        Object cachedRecipeData = readField(logic, "cachedRecipeData");
        if (cachedRecipeData == null) {
            throw new IllegalStateException("GTCEu CachedRecipeData is unavailable");
        }

        Method setRecipe = findMethod(
                cachedRecipeData.getClass(), "setRecipe", IRecipe.class);
        if (setRecipe == null) {
            throw new NoSuchMethodException("CachedRecipeData#setRecipe(IRecipe)");
        }

        RecipeSelectionSeeder.clear(binding.executionMatrix);
        setRecipe.invoke(cachedRecipeData, new Object[] { null });
        Field cachedRecipe = findField(logic.getClass(), "cachedRecipe");
        if (cachedRecipe != null) {
            cachedRecipe.set(logic, null);
        }
        invokeRequired(logic, "updateCurrentRecipe");

        if (!isClient(binding)) {
            Method detect = findMethod(logic.getClass(), "detectAndSendChanges", boolean.class);
            if (detect != null) {
                detect.invoke(logic, Boolean.FALSE);
            }
        }
    }

    private static void applyCeuSelection(Binding binding, IRecipe selectedRecipe)
            throws ReflectiveOperationException {
        Object logic = binding.engine;
        Object cache = readField(logic, "cachedRecipeData");
        if (cache == null) {
            throw new IllegalStateException("GTCEu CachedRecipeData is unavailable");
        }
        Method setter = findMethod(cache.getClass(), "setRecipe", IRecipe.class);
        Method getter = findMethod(cache.getClass(), "getRecipe");
        Object resultValue = invoke(logic, "getCraftingResultInventory");
        if (setter == null || getter == null || !(resultValue instanceof IInventory)) {
            throw new IllegalStateException("GTCEu recipe cache cannot be updated and restored safely");
        }
        IInventory result = (IInventory) resultValue;
        ItemStack output = RecipeProbe.craftingResult(selectedRecipe, binding.matrix);
        if (output.isEmpty()) {
            throw new IllegalStateException("Selected GTCEu recipe has no crafting result");
        }
        Field cachedRecipe = findField(logic.getClass(), "cachedRecipe");
        Field oldResult = findField(logic.getClass(), "oldResult");
        Object previousRecipe = getter.invoke(cache);
        Object previousCached = cachedRecipe == null ? null : cachedRecipe.get(logic);
        Object previousOldResult = oldResult == null ? null : oldResult.get(logic);
        ItemStack previousOutput = result.getStackInSlot(0).copy();
        ItemStack[] previousInputs = new ItemStack[binding.executionMatrix.getSizeInventory()];
        for (int slot = 0; slot < previousInputs.length; slot++) {
            previousInputs[slot] = binding.executionMatrix.getStackInSlot(slot).copy();
        }
        try {
            Method synchronize = findMethod(logic.getClass(), "hasCraftingGridUpdated");
            if (synchronize != null) {
                synchronize.invoke(logic);
            }
            setter.invoke(cache, selectedRecipe);
            if (binding.matrix != binding.executionMatrix) {
                for (int slot = 0; slot < binding.matrix.getSizeInventory(); slot++) {
                    ItemStack template = binding.matrix.getStackInSlot(slot);
                    if (!ItemStack.areItemStacksEqual(template, binding.executionMatrix.getStackInSlot(slot))) {
                        binding.executionMatrix.setInventorySlotContents(slot, template.copy());
                    }
                }
            }
            if (cachedRecipe != null) {
                cachedRecipe.set(logic, selectedRecipe);
            }
            if (oldResult != null) {
                oldResult.set(logic, output.copy());
            }
            result.setInventorySlotContents(0, output.copy());
            if (!isClient(binding)) {
                Method detect = findMethod(logic.getClass(), "detectAndSendChanges", boolean.class);
                if (detect != null) {
                    detect.invoke(logic, Boolean.FALSE);
                }
            }
            RecipeSelectionSeeder.seed(binding.executionMatrix, selectedRecipe.getRegistryName());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            try {
                setter.invoke(cache, previousRecipe);
                if (cachedRecipe != null) { cachedRecipe.set(logic, previousCached); }
                if (oldResult != null) { oldResult.set(logic, previousOldResult); }
                for (int slot = 0; slot < previousInputs.length; slot++) {
                    binding.executionMatrix.setInventorySlotContents(slot, previousInputs[slot]);
                }
                result.setInventorySlotContents(0, previousOutput);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    private static boolean isClient(Binding binding) {
        if (binding.engine != null) {
            try {
                Field worldField = findField(binding.engine.getClass(), "world");
                if (worldField != null) {
                    Object world = worldField.get(binding.engine);
                    if (world instanceof World) {
                        return ((World) world).isRemote;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (binding.container != null) {
            try {
                Field syncManagerField = findField(binding.container.getClass(), "syncManager");
                Object syncManager = syncManagerField != null ? syncManagerField.get(binding.container) : null;
                if (syncManager == null) {
                    syncManager = invoke(binding.container, "getSyncManager");
                }
                if (syncManager != null) {
                    Object clientVal = invoke(syncManager, "isClient");
                    if (clientVal instanceof Boolean) {
                        return ((Boolean) clientVal).booleanValue();
                    }
                }
                Object player = invoke(binding.container, "getPlayer");
                if (player instanceof EntityPlayer) {
                    return ((EntityPlayer) player).world.isRemote;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            return net.minecraftforge.fml.common.FMLCommonHandler.instance().getEffectiveSide().isClient();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void refreshCe(Binding binding, @Nullable IRecipe selectedRecipe)
            throws ReflectiveOperationException {
        Object resolver = binding.engine;
        Field cachedRecipe = findField(resolver.getClass(), "cachedRecipe");
        if (cachedRecipe == null) {
            throw new NoSuchFieldException("CraftingRecipeResolver#cachedRecipe");
        }

        invokeRequired(resolver, "updateInventoryCrafting");

        if (selectedRecipe != null) {
            Object resultValue = invoke(resolver, "getCraftingResultInventory");
            if (!(resultValue instanceof IInventory)) {
                throw new IllegalStateException("GTCE crafting result inventory is unavailable");
            }
            ItemStack output = RecipeProbe.craftingResult(selectedRecipe, binding.matrix);
            if (output.isEmpty()) {
                throw new IllegalStateException("Selected GTCE recipe has no crafting result");
            }
            IInventory result = (IInventory) resultValue;
            Field cacheField = findField(resolver.getClass(), "cachedRecipeData");
            if (cacheField == null) {
                throw new NoSuchFieldException("CraftingRecipeResolver#cachedRecipeData");
            }
            Object sourceList = invoke(resolver, "getItemSourceList");
            Object cache = createCeRecipeCache(cacheField.getType(), sourceList, selectedRecipe, output);
            Object cacheInventory = readField(cache, "inventory");
            if (!(cacheInventory instanceof InventoryCrafting)) {
                throw new IllegalStateException("GTCE ingredient cache has no crafting inventory");
            }
            InventoryCrafting ingredients = (InventoryCrafting) cacheInventory;
            for (int slot = 0; slot < binding.matrix.getSizeInventory(); slot++) {
                ingredients.setInventorySlotContents(slot, binding.matrix.getStackInSlot(slot).copy());
            }
            // Native matching builds extraction requirements and ingredient substitutes.
            // An unavailable ingredient may reject crafting, but not recipe selection.
            invokeRequired(cache, "attemptMatchRecipe");
            Object previousRecipe = cachedRecipe.get(resolver);
            Object previousCache = cacheField.get(resolver);
            ItemStack previousOutput = result.getStackInSlot(0).copy();
            try {
                cachedRecipe.set(resolver, selectedRecipe);
                cacheField.set(resolver, cache);
                result.setInventorySlotContents(0, output.copy());
                RecipeSelectionSeeder.seed(binding.executionMatrix, selectedRecipe.getRegistryName());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                try {
                    cachedRecipe.set(resolver, previousRecipe);
                    cacheField.set(resolver, previousCache);
                    result.setInventorySlotContents(0, previousOutput);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        } else {
            RecipeSelectionSeeder.clear(binding.executionMatrix);
            cachedRecipe.set(resolver, null);
            Field cacheField = findField(resolver.getClass(), "cachedRecipeData");
            if (cacheField != null) {
                cacheField.set(resolver, null);
            }
            binding.resultSlot.inventory.setInventorySlotContents(0, ItemStack.EMPTY);
            invokeRequired(resolver, "updateCurrentRecipe");
        }
    }

    private static Object createCeRecipeCache(Class<?> cacheType, Object sources, IRecipe recipe, ItemStack output)
            throws ReflectiveOperationException {
        for (Constructor<?> constructor : cacheType.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 3 && sources != null && parameters[0].isInstance(sources)
                    && parameters[1] == IRecipe.class && parameters[2] == ItemStack.class) {
                constructor.setAccessible(true);
                return constructor.newInstance(sources, recipe, output.copy());
            }
        }
        throw new NoSuchMethodException(cacheType.getName() + "(ItemSourceList, IRecipe, ItemStack)");
    }

    @Nullable
    static Object invoke(Object target, String name)
            throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), name);
        return method == null ? null : method.invoke(target);
    }

    @Nullable
    private static Object invoke(
            Object target,
            String name,
            Class<?> parameterType,
            Object argument) throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), name, parameterType);
        return method == null ? null : method.invoke(target, argument);
    }

    private static void invokeRequired(Object target, String name)
            throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            throw new NoSuchMethodException(target.getClass().getName() + '#' + name);
        }
        method.invoke(target);
    }

    @Nullable
    static Object readField(Object target, String name)
            throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        return field == null ? null : field.get(target);
    }

    @Nullable
    static Method findMethod(
            Class<?> type,
            String name,
            Class<?>... parameterTypes) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static boolean hasClassInHierarchy(Class<?> type, String className) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (className.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    static final class Binding {
        final Container container;
        final Object engine;
        final InventoryCrafting matrix;
        final InventoryCrafting executionMatrix;
        final Slot resultSlot;
        final boolean ceu;

        private Binding(
                Container container,
                Object engine,
                InventoryCrafting matrix,
                Slot resultSlot,
                boolean ceu) {
            this(container, engine, matrix, resultSlot, ceu, matrix);
        }

        private Binding(Container container, Object engine, InventoryCrafting matrix,
                Slot resultSlot, boolean ceu, InventoryCrafting executionMatrix) {
            this.container = container;
            this.engine = engine;
            this.matrix = matrix;
            this.executionMatrix = executionMatrix;
            this.resultSlot = resultSlot;
            this.ceu = ceu;
        }
    }
}
