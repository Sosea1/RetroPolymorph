package dev.sosea1.retropolymorph.compat.rsb;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time reflective discovery and cached typed member handles for Retro Sophisticated Backpacks
 * and ModularUI. All cold-path discovery fails safely and is cached to prevent repeated lookups.
 */
public final class RsbBindings {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    // Sentinel to represent a lookup that definitively returned null (no such field/method)
    private static final Method NO_SUCH_METHOD;
    private static final Field NO_SUCH_FIELD;

    static {
        Method mSentinel = null;
        Field fSentinel = null;
        try {
            mSentinel = RsbBindings.class.getDeclaredMethod("sentinelMethod");
            fSentinel = RsbBindings.class.getDeclaredField("LOGGER");
        } catch (NoSuchMethodException | NoSuchFieldException | SecurityException | LinkageError error) {
            throw new ExceptionInInitializerError(error);
        }
        NO_SUCH_METHOD = mSentinel;
        NO_SUCH_FIELD = fSentinel;
    }

    private static void sentinelMethod() {
    }

    public static final String CONTAINER_CLASS =
            "com.cleanroommc.retrosophisticatedbackpacks.common.gui.BackpackContainer";

    // Method/Field caches
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<String, Method>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<String, Field>();

    // Container fields
    private static volatile boolean containerResolved;
    private static volatile Field matricesField;
    private static volatile Field resultsField;
    private static volatile Field wrapperField;

    // RSB Capabilities
    private static volatile boolean rsbCapabilitiesResolved;
    private static volatile Object rsbCraftingCapability;
    private static volatile Object rsbUpgradeCapability;

    // ExposedItemStackHandler (NBT deserialization)
    private static volatile boolean exposedHandlerResolved;
    private static volatile Constructor<?> exposedHandlerConstructor;
    private static volatile Method exposedHandlerDeserialize;

    // ModularUI Screen / Panels
    private static volatile boolean modularUiResolved;
    private static volatile Class<?> guiContainerWrapperClass;
    private static volatile Method getScreenMethod;
    private static volatile Method getMainPanelMethod;
    private static volatile Method getPanelManagerMethod;
    private static volatile Method getOpenPanelsMethod;
    private static volatile Method getChildrenMethod;

    private RsbBindings() {
    }

    public static boolean resolveContainer(Class<?> clazz) {
        if (!containerResolved) {
            synchronized (RsbBindings.class) {
                if (!containerResolved) {
                    try {
                        Class<?> target = clazz;
                        if (!CONTAINER_CLASS.equals(target.getName())) {
                            target = Class.forName(CONTAINER_CLASS, false, RsbBindings.class.getClassLoader());
                        }
                        matricesField = findField(target, "inventoryCraftingInstances");
                        resultsField = findField(target, "craftingSlotInstances");
                        Field wf = findField(target, "wrapper");
                        if (wf == null) {
                            wf = findField(target, "backpackWrapper");
                        }
                        wrapperField = wf;
                        if (matricesField == null || resultsField == null) {
                            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                                    "retrosophisticatedbackpacks",
                                    "resolveContainer",
                                    new NoSuchFieldException("inventoryCraftingInstances/craftingSlotInstances"));
                        }
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError t) {
                        dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                                "retrosophisticatedbackpacks",
                                "resolveContainer",
                                t);
                    }
                    containerResolved = true;
                }
            }
        }
        return matricesField != null && resultsField != null;
    }

    @Nullable
    public static Map<?, ?> getMatrices(net.minecraft.inventory.Container container) {
        if (container == null || !resolveContainer(container.getClass()) || matricesField == null) {
            return null;
        }
        try {
            Object val = matricesField.get(container);
            return val instanceof Map ? (Map<?, ?>) val : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError t) {
            return null;
        }
    }

    @Nullable
    public static Map<?, ?> getResults(net.minecraft.inventory.Container container) {
        if (container == null || !resolveContainer(container.getClass()) || resultsField == null) {
            return null;
        }
        try {
            Object val = resultsField.get(container);
            return val instanceof Map ? (Map<?, ?>) val : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError t) {
            return null;
        }
    }

    @Nullable
    public static Object getBackpackWrapper(net.minecraft.inventory.Container container) {
        if (container == null || !resolveContainer(container.getClass())) {
            return null;
        }
        if (wrapperField != null) {
            try {
                Object val = wrapperField.get(container);
                if (val != null) {
                    return val;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            }
        }
        // Fallback: search declared fields on container class
        for (Class<?> cur = container.getClass(); cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            for (Field f : cur.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object val = f.get(container);
                    if (val != null && val.getClass().getName().endsWith(".BackpackWrapper")) {
                        wrapperField = f;
                        return val;
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }
        }
        return null;
    }

    @Nullable
    public static Method findNoArgMethod(Class<?> clazz, String name) {
        if (clazz == null || name == null) {
            return null;
        }
        String key = clazz.getName() + '#' + name + "()";
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) {
            return cached == NO_SUCH_METHOD ? null : cached;
        }

        Method resolved = null;
        for (Class<?> cur = clazz; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            try {
                Method m = cur.getDeclaredMethod(name);
                m.setAccessible(true);
                resolved = m;
                break;
            } catch (NoSuchMethodException ignored) {
            } catch (SecurityException | LinkageError ignored) {
                break;
            }
        }

        METHOD_CACHE.put(key, resolved != null ? resolved : NO_SUCH_METHOD);
        return resolved;
    }

    @Nullable
    public static Field findField(Class<?> clazz, String name) {
        if (clazz == null || name == null) {
            return null;
        }
        String key = clazz.getName() + '#' + name;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached == NO_SUCH_FIELD ? null : cached;
        }

        Field resolved = null;
        for (Class<?> cur = clazz; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
            try {
                Field f = cur.getDeclaredField(name);
                f.setAccessible(true);
                resolved = f;
                break;
            } catch (NoSuchFieldException ignored) {
            } catch (SecurityException | LinkageError ignored) {
                break;
            }
        }

        FIELD_CACHE.put(key, resolved != null ? resolved : NO_SUCH_FIELD);
        return resolved;
    }

    @Nullable
    public static Object invokeNoArg(@Nullable Object target, String name) {
        if (target == null) {
            return null;
        }
        Method method = findNoArgMethod(target.getClass(), name);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    public static Object getRsbCraftingCapability() {
        resolveCapabilities();
        return rsbCraftingCapability;
    }

    @Nullable
    public static Object getRsbUpgradeCapability() {
        resolveCapabilities();
        return rsbUpgradeCapability;
    }

    private static void resolveCapabilities() {
        if (!rsbCapabilitiesResolved) {
            synchronized (RsbBindings.class) {
                if (!rsbCapabilitiesResolved) {
                    try {
                        Class<?> capClass = Class.forName(
                                "com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities",
                                false,
                                RsbBindings.class.getClassLoader());
                        Field craftField = capClass.getField("CRAFTING_ITEM_HANDLER_CAPABILITY");
                        rsbCraftingCapability = craftField.get(null);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                        dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                                "retrosophisticatedbackpacks",
                                "resolveCraftingCapability",
                                error);
                    }
                    try {
                        Class<?> capClass = Class.forName(
                                "com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities",
                                false,
                                RsbBindings.class.getClassLoader());
                        Field upgField = capClass.getField("UPGRADE_CAPABILITY");
                        rsbUpgradeCapability = upgField.get(null);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                        dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                                "retrosophisticatedbackpacks",
                                "resolveUpgradeCapability",
                                error);
                    }
                    rsbCapabilitiesResolved = true;
                }
            }
        }
    }

    @Nullable
    public static IItemHandler extractCraftMatrixFromStack(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Object cap = getRsbCraftingCapability();
        if (cap instanceof net.minecraftforge.common.capabilities.Capability) {
            try {
                Object wrapper = stack.getCapability((net.minecraftforge.common.capabilities.Capability<?>) cap, null);
                if (wrapper != null) {
                    Object matrix = invokeNoArg(wrapper, "getCraftMatrix");
                    if (matrix instanceof IItemHandler) {
                        return (IItemHandler) matrix;
                    }
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return null;
    }

    public static boolean isUpgradeTabOpened(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Object cap = getRsbUpgradeCapability();
        if (cap instanceof net.minecraftforge.common.capabilities.Capability) {
            try {
                Object wrapper = stack.getCapability((net.minecraftforge.common.capabilities.Capability<?>) cap, null);
                if (wrapper != null) {
                    Object res = invokeNoArg(wrapper, "isTabOpened");
                    if (res instanceof Boolean) {
                        return ((Boolean) res).booleanValue();
                    }
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return false;
    }

    @Nullable
    public static IItemHandler deserializeExposedHandler(NBTTagCompound nbt, int slots) {
        if (!exposedHandlerResolved) {
            synchronized (RsbBindings.class) {
                if (!exposedHandlerResolved) {
                    try {
                        Class<?> handlerClass = Class.forName(
                                "com.cleanroommc.retrosophisticatedbackpacks.inventory.ExposedItemStackHandler",
                                false,
                                RsbBindings.class.getClassLoader());
                        exposedHandlerConstructor = handlerClass.getConstructor(int.class);
                        exposedHandlerDeserialize = handlerClass.getMethod("deserializeNBT", NBTTagCompound.class);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                        dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                                "retrosophisticatedbackpacks",
                                "deserializeExposedHandler",
                                error);
                    }
                    exposedHandlerResolved = true;
                }
            }
        }

        if (exposedHandlerConstructor == null || exposedHandlerDeserialize == null) {
            return null;
        }

        try {
            Object handler = exposedHandlerConstructor.newInstance(Integer.valueOf(slots));
            exposedHandlerDeserialize.invoke(handler, nbt);
            return handler instanceof IItemHandler ? (IItemHandler) handler : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static void resolveModularUi() {
        if (!modularUiResolved) {
            synchronized (RsbBindings.class) {
                if (!modularUiResolved) {
                    try {
                        guiContainerWrapperClass = Class.forName(
                                "com.cleanroommc.modularui.screen.GuiContainerWrapper",
                                false,
                                RsbBindings.class.getClassLoader());
                        getScreenMethod = guiContainerWrapperClass.getMethod("getScreen");
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                        dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                                "retrosophisticatedbackpacks",
                                "resolveModularUi",
                                error);
                    }
                    modularUiResolved = true;
                }
            }
        }
    }

    @Nullable
    public static Class<?> getGuiContainerWrapperClass() {
        resolveModularUi();
        return guiContainerWrapperClass;
    }

    @Nullable
    public static Object getModularScreen(Object screenWrapper) {
        resolveModularUi();
        if (screenWrapper == null || getScreenMethod == null || guiContainerWrapperClass == null) {
            return null;
        }
        if (!guiContainerWrapperClass.isInstance(screenWrapper)) {
            return null;
        }
        try {
            return getScreenMethod.invoke(screenWrapper);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static final class SlotBinding {
        @Nullable
        public final Method isEnabledMethod;
        @Nullable
        public final Method upgradeIndexMethod;
        @Nullable
        public final Field upgradeIndexField;
        @Nullable
        public volatile Field matrixField;
        @Nullable
        public final Field[] candidateMatrixFields;
        @Nullable
        public final Method matrixMethod;

        public SlotBinding(
                @Nullable Method isEnabledMethod,
                @Nullable Method upgradeIndexMethod,
                @Nullable Field upgradeIndexField,
                @Nullable Field matrixField,
                @Nullable Field[] candidateMatrixFields,
                @Nullable Method matrixMethod) {
            this.isEnabledMethod = isEnabledMethod;
            this.upgradeIndexMethod = upgradeIndexMethod;
            this.upgradeIndexField = upgradeIndexField;
            this.matrixField = matrixField;
            this.candidateMatrixFields = candidateMatrixFields;
            this.matrixMethod = matrixMethod;
        }

        public boolean isEnabled(Slot slot) {
            if (this.isEnabledMethod == null) {
                return true;
            }
            try {
                Object val = this.isEnabledMethod.invoke(slot);
                return !(val instanceof Boolean) || ((Boolean) val).booleanValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return true;
            }
        }

        public int readUpgradeIndex(Slot slot) {
            if (this.upgradeIndexMethod != null) {
                try {
                    Object val = this.upgradeIndexMethod.invoke(slot);
                    if (val instanceof Number) {
                        return ((Number) val).intValue();
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }
            if (this.upgradeIndexField != null) {
                try {
                    Object val = this.upgradeIndexField.get(slot);
                    if (val instanceof Number) {
                        return ((Number) val).intValue();
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }
            return slot.slotNumber;
        }

        @Nullable
        public InventoryCrafting readCraftMatrix(Slot slot) {
            Field f = this.matrixField;
            if (f != null) {
                try {
                    Object val = f.get(slot);
                    if (val instanceof InventoryCrafting) {
                        return (InventoryCrafting) val;
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }
            if (this.matrixMethod != null) {
                try {
                    Object val = this.matrixMethod.invoke(slot);
                    if (val instanceof InventoryCrafting) {
                        return (InventoryCrafting) val;
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }
            if (f == null && this.candidateMatrixFields != null && this.candidateMatrixFields.length > 0) {
                for (Field cand : this.candidateMatrixFields) {
                    try {
                        Object val = cand.get(slot);
                        if (val instanceof InventoryCrafting) {
                            this.matrixField = cand; // Promoted and cached for this slot class
                            return (InventoryCrafting) val;
                        }
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    }
                }
            }
            return null;
        }
    }

    private static final Map<Class<?>, SlotBinding> SLOT_BINDINGS =
            new ConcurrentHashMap<Class<?>, SlotBinding>();

    public static SlotBinding getSlotBinding(Class<?> slotClass) {
        SlotBinding binding = SLOT_BINDINGS.get(slotClass);
        if (binding == null) {
            binding = discoverSlotBinding(slotClass);
            SLOT_BINDINGS.put(slotClass, binding);
        }
        return binding;
    }

    private static SlotBinding discoverSlotBinding(Class<?> clazz) {
        Method isEnabled = null;
        try {
            isEnabled = clazz.getMethod("isEnabled");
        } catch (NoSuchMethodException ignored) {
        } catch (SecurityException | LinkageError ignored) {
        }

        Method upgradeMethod = null;
        try {
            upgradeMethod = clazz.getMethod("getUpgradeSlotIndex");
        } catch (NoSuchMethodException ignored) {
        } catch (SecurityException | LinkageError ignored) {
        }

        Field upgradeField = findField(clazz, "upgradeSlotIndex");

        Field matrixField = null;
        List<Field> candidateFields = new ArrayList<Field>();
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class && matrixField == null) {
            Field[] fields;
            try {
                fields = cur.getDeclaredFields();
            } catch (SecurityException | LinkageError t) {
                fields = new Field[0];
            }
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (InventoryCrafting.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        matrixField = f;
                        break;
                    } catch (SecurityException | LinkageError ignored) {
                    }
                } else if (!f.getType().isPrimitive()) {
                    try {
                        f.setAccessible(true);
                        candidateFields.add(f);
                    } catch (SecurityException | LinkageError ignored) {
                    }
                }
            }
            cur = cur.getSuperclass();
        }

        Method matrixMethod = null;
        String[] getterNames = {"getCraftMatrix", "getCraftingMatrix", "getInventoryCrafting"};
        for (String name : getterNames) {
            try {
                Method m = clazz.getMethod(name);
                if (m.getParameterTypes().length == 0 && InventoryCrafting.class.isAssignableFrom(m.getReturnType())) {
                    matrixMethod = m;
                    break;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (SecurityException | LinkageError ignored) {
            }
        }

        Field[] candidates = matrixField != null || candidateFields.isEmpty()
                ? null
                : candidateFields.toArray(new Field[candidateFields.size()]);
        return new SlotBinding(isEnabled, upgradeMethod, upgradeField, matrixField, candidates, matrixMethod);
    }
}
