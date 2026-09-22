package dev.sosea1.retropolymorph.compat.enderio;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Cached reflection for Ender IO's optional Crafter classes. */
final class EnderIoCrafterReflection {

    private static final String CONTAINER =
            "crazypants.enderio.machines.machine.crafter.ContainerCrafter";

    private static final ClassValue<Method> TILE_METHODS = new ClassValue<Method>() {
        @Override
        protected Method computeValue(Class<?> type) {
            return findMethod(type, "getTileEntityNN");
        }
    };

    private static final ClassValue<Field> GRID_FIELDS = new ClassValue<Field>() {
        @Override
        protected Field computeValue(Class<?> type) {
            return findField(type, "craftingGrid");
        }
    };

    private static final ClassValue<Method> REFRESH_METHODS = new ClassValue<Method>() {
        @Override
        protected Method computeValue(Class<?> type) {
            return findMethod(type, "updateCraftingOutput");
        }
    };

    private EnderIoCrafterReflection() {
    }

    static boolean isCrafterContainer(Container container) {
        return container != null && hasClassInHierarchy(container.getClass(), CONTAINER);
    }

    @Nullable
    static TileEntity getTile(Container container) {
        if (!isCrafterContainer(container)) {
            return null;
        }
        try {
            Method method = TILE_METHODS.get(container.getClass());
            Object value = method == null ? null : method.invoke(container);
            return value instanceof TileEntity ? (TileEntity) value : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure("enderio", "getTile", exception);
            return null;
        }
    }

    @Nullable
    static IInventory getCraftingGrid(TileEntity tile) {
        if (tile == null) {
            return null;
        }
        try {
            Field field = GRID_FIELDS.get(tile.getClass());
            Object value = field == null ? null : field.get(tile);
            return value instanceof IInventory ? (IInventory) value : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure("enderio", "getCraftingGrid", exception);
            return null;
        }
    }

    static void refreshOutput(TileEntity tile) {
        if (tile == null) {
            return;
        }
        try {
            Method method = REFRESH_METHODS.get(tile.getClass());
            if (method != null) {
                method.invoke(tile);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure("enderio", "refreshOutput", ignored);
        }
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == 0) {
                    method.setAccessible(true);
                    return method;
                }
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

    private static boolean hasClassInHierarchy(Class<?> type, String targetName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (targetName.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }
}
