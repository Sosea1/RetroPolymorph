package dev.sosea1.retropolymorph.client;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;

/**
 * Best-effort identity bridge for JEI/HEI 1.12 recipe transfer.
 *
 * Legacy JEI exposes ingredients publicly but not a universal recipe identifier.
 * Its RecipeLayout still owns the wrapper that created those ingredients, and the
 * vanilla/modded crafting wrappers commonly retain the backing IRecipe. We inspect
 * that object conservatively and only return an id when exactly one distinct IRecipe
 * is present. Any incompatible JEI fork simply falls back to output matching.
 */
public final class JeiRecipeIdentity {

    private static final String WRAPPER_FIELD = "recipeWrapper";

    private JeiRecipeIdentity() {
    }

    @Nullable
    public static String tryExtractRecipeKey(Object recipeLayout) {
        if (recipeLayout == null) {
            return null;
        }

        try {
            Object wrapper = readWrapper(recipeLayout);
            IRecipe recipe = findSingleRecipe(wrapper);
            if (recipe == null) {
                return null;
            }
            ResourceLocation id = recipe.getRegistryName();
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static Object readWrapper(Object layout) throws IllegalAccessException {
        Class<?> type = layout.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(WRAPPER_FIELD);
                field.setAccessible(true);
                return field.get(layout);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    @Nullable
    private static IRecipe findSingleRecipe(Object wrapper) throws IllegalAccessException {
        if (wrapper == null) {
            return null;
        }
        if (wrapper instanceof IRecipe) {
            return (IRecipe) wrapper;
        }

        IdentityHashMap<IRecipe, Boolean> found = new IdentityHashMap<IRecipe, Boolean>();
        Class<?> type = wrapper.getClass();
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())
                        || !IRecipe.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(wrapper);
                if (value instanceof IRecipe) {
                    found.put((IRecipe) value, Boolean.TRUE);
                    if (found.size() > 1) {
                        return null;
                    }
                }
            }
            type = type.getSuperclass();
        }

        return found.size() == 1 ? found.keySet().iterator().next() : null;
    }
}
