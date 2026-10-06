package com.sosea1.retropolymorph.core;

import com.sosea1.retropolymorph.compat.tconstruct.TinkersCraftingStationAccess;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles clearing cached recipes in FastWorkbench-style containers
 * (FastWorkbench, Tinkers Construct Crafting Station, standalone Crafting Station mod, etc.).
 */
public final class ContainerRecipeCacheHelper {

    private static final Map<Class<?>, CachedFields> FIELD_CACHE = new ConcurrentHashMap<Class<?>, CachedFields>();

    private static final class CachedFields {
        @Nullable final Field lastRecipe;
        @Nullable final Field lastLastRecipe;

        CachedFields(@Nullable Field lastRecipe, @Nullable Field lastLastRecipe) {
            this.lastRecipe = lastRecipe;
            this.lastLastRecipe = lastLastRecipe;
        }
    }

    private ContainerRecipeCacheHelper() {
    }

    public static void clearLastRecipe(@Nullable Container container) {
        if (container == null) {
            return;
        }

        if (container instanceof TinkersCraftingStationAccess) {
            ((TinkersCraftingStationAccess) container).retropolymorph$clearLastRecipe();
            return;
        }

        Class<?> clazz = container.getClass();
        CachedFields fields = FIELD_CACHE.get(clazz);
        if (fields == null) {
            fields = findFields(clazz);
            FIELD_CACHE.put(clazz, fields);
        }
        if (fields.lastRecipe != null) {
            try {
                fields.lastRecipe.set(container, null);
            } catch (Throwable ignored) {
            }
        }
        if (fields.lastLastRecipe != null) {
            try {
                fields.lastLastRecipe.set(container, null);
            } catch (Throwable ignored) {
            }
        }
    }

    private static CachedFields findFields(Class<?> clazz) {
        Field last = findField(clazz, "lastRecipe");
        Field lastLast = findField(clazz, "lastLastRecipe");
        if (last != null) {
            last.setAccessible(true);
        }
        if (lastLast != null) {
            lastLast.setAccessible(true);
        }
        return new CachedFields(last, lastLast);
    }

    @Nullable
    private static Field findField(Class<?> clazz, String fieldName) {
        for (Class<?> current = clazz; current != null && current != Container.class; current = current.getSuperclass()) {
            try {
                Field f = current.getDeclaredField(fieldName);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
