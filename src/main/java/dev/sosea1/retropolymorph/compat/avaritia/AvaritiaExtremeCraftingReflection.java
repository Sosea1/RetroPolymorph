package dev.sosea1.retropolymorph.compat.avaritia;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import dev.sosea1.retropolymorph.core.TransientSelectionStore;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class AvaritiaExtremeCraftingReflection {

    private static final String CONTAINER =
            "morph.avaritia.container.ContainerExtremeCrafting";
    private static final String MANAGER =
            "morph.avaritia.recipe.AvaritiaRecipeManager";
    private static final TransientSelectionStore SELECTIONS = new TransientSelectionStore();

    private static volatile Field recipesField;

    private static final ClassValue<OwnerField> OWNER_FIELDS = new ClassValue<OwnerField>() {
        @Override
        protected OwnerField computeValue(Class<?> type) {
            return new OwnerField(findField(type, "craft"));
        }
    };

    private static final ClassValue<RecipeMethods> RECIPE_METHODS = new ClassValue<RecipeMethods>() {
        @Override
        protected RecipeMethods computeValue(Class<?> type) {
            return new RecipeMethods(
                    findMethod(type, "matches", InventoryCrafting.class, World.class),
                    findMethod(type, "getCraftingResult", InventoryCrafting.class),
                    findMethod(type, "getRemainingItems", InventoryCrafting.class));
        }
    };

    private AvaritiaExtremeCraftingReflection() {
    }

    static boolean isContainer(@Nullable Container container) {
        return container != null && hasClassInHierarchy(container.getClass(), CONTAINER);
    }

    @Nullable
    static Binding bind(Container container) {
        if (!isContainer(container)) {
            return null;
        }

        InventoryCrafting matrix = null;
        Slot result = null;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof InventoryCrafting) {
                InventoryCrafting candidate = (InventoryCrafting) slot.inventory;
                if (candidate.getWidth() == 9 && candidate.getHeight() == 9) {
                    if (matrix != null && matrix != candidate) {
                        return null;
                    }
                    matrix = candidate;
                }
            }
            if (slot.inventory instanceof InventoryCraftResult) {
                if (result != null && result != slot) {
                    return null;
                }
                result = slot;
            }
        }

        if (matrix == null || result == null) {
            return null;
        }
        Object owner = ownerOf(matrix);
        return new Binding(matrix, result, owner == null ? matrix : owner);
    }

    static List<RecipeOption> findOptions(Binding binding, World world) {
        Map<?, ?> recipes = recipes();
        if (recipes == null || recipes.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<RecipeOption> result = new ArrayList<RecipeOption>();
        try {
            for (Map.Entry<?, ?> entry : new ArrayList<Map.Entry<?, ?>>(recipes.entrySet())) {
                if (!(entry.getKey() instanceof ResourceLocation) || entry.getValue() == null) {
                    continue;
                }
                Object recipe = entry.getValue();
                if (!matches(recipe, binding.matrix, world)) {
                    continue;
                }
                ItemStack output = craftingResult(recipe, binding.matrix);
                if (output == null || output.isEmpty()) {
                    continue;
                }
                result.add(new RecipeOption(entry.getKey().toString(), output));
            }
        } catch (RuntimeException | LinkageError exception) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "avaritia", "enumerateExtremeRecipes", exception);
            return Collections.emptyList();
        }
        return result;
    }

    static boolean select(Binding binding, String recipeKey, World world) {
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        Map<?, ?> recipes = recipes();
        if (id == null || recipes == null) {
            return false;
        }
        Object recipe = recipes.get(id);
        if (recipe == null || !matches(recipe, binding.matrix, world)) {
            return false;
        }
        SELECTIONS.set(binding.owner, id.toString());
        return true;
    }

    static void clear(Object owner) {
        SELECTIONS.clear(owner);
    }

    @Nullable
    static String getSelectedKey(Object owner) {
        return SELECTIONS.get(owner);
    }

    static void setSelectedKey(Object owner, @Nullable String key) {
        SELECTIONS.set(owner, RecipeKey.isWireSafe(key) ? key : null);
    }

    public static boolean hasSelection(InventoryCrafting matrix) {
        return SELECTIONS.get(ownerKey(matrix)) != null;
    }

    @Nullable
    public static ItemStack selectedCraftingResult(InventoryCrafting matrix, World world) {
        Object recipe = selectedRecipe(matrix, world);
        return recipe == null ? null : craftingResult(recipe, matrix);
    }

    @Nullable
    public static NonNullList<ItemStack> selectedRemainingItems(
            InventoryCrafting matrix,
            World world) {
        Object recipe = selectedRecipe(matrix, world);
        if (recipe == null) {
            return null;
        }
        RecipeMethods methods = RECIPE_METHODS.get(recipe.getClass());
        if (methods.remaining == null) {
            return null;
        }
        try {
            Object value = methods.remaining.invoke(recipe, matrix);
            if (value instanceof NonNullList) {
                @SuppressWarnings("unchecked")
                NonNullList<ItemStack> result = (NonNullList<ItemStack>) value;
                return result;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "avaritia", "selectedRemainingItems", exception);
        }
        return null;
    }

    @Nullable
    private static Object selectedRecipe(InventoryCrafting matrix, World world) {
        Object owner = ownerKey(matrix);
        String key = SELECTIONS.get(owner);
        ResourceLocation id = RecipeKey.parseForgeId(key);
        Map<?, ?> recipes = recipes();
        if (id == null || recipes == null) {
            SELECTIONS.clear(owner);
            return null;
        }
        Object recipe = recipes.get(id);
        if (recipe == null || !matches(recipe, matrix, world)) {
            SELECTIONS.clear(owner);
            return null;
        }
        return recipe;
    }

    private static boolean matches(Object recipe, InventoryCrafting matrix, World world) {
        RecipeMethods methods = RECIPE_METHODS.get(recipe.getClass());
        if (methods.matches == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(methods.matches.invoke(recipe, matrix, world));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    @Nullable
    private static ItemStack craftingResult(Object recipe, InventoryCrafting matrix) {
        RecipeMethods methods = RECIPE_METHODS.get(recipe.getClass());
        if (methods.result == null) {
            return null;
        }
        try {
            Object value = methods.result.invoke(recipe, matrix);
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    private static Map<?, ?> recipes() {
        try {
            Field field = recipesField;
            if (field == null) {
                synchronized (AvaritiaExtremeCraftingReflection.class) {
                    field = recipesField;
                    if (field == null) {
                        Class<?> manager = Class.forName(MANAGER);
                        field = manager.getField("EXTREME_RECIPES");
                        field.setAccessible(true);
                        recipesField = field;
                    }
                }
            }
            Object value = field.get(null);
            return value instanceof Map ? (Map<?, ?>) value : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "avaritia", "extremeRecipeRegistry", exception);
            return null;
        }
    }

    private static Object ownerKey(InventoryCrafting matrix) {
        Object owner = ownerOf(matrix);
        return owner == null ? matrix : owner;
    }

    @Nullable
    private static Object ownerOf(InventoryCrafting matrix) {
        if (matrix == null) {
            return null;
        }
        Field field = OWNER_FIELDS.get(matrix.getClass()).field;
        if (field == null) {
            return null;
        }
        try {
            return field.get(matrix);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            Method method = type.getMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
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
        final InventoryCrafting matrix;
        final Slot resultSlot;
        final Object owner;

        private Binding(InventoryCrafting matrix, Slot resultSlot, Object owner) {
            this.matrix = matrix;
            this.resultSlot = resultSlot;
            this.owner = owner;
        }
    }

    private static final class OwnerField {
        final Field field;

        private OwnerField(@Nullable Field field) {
            this.field = field;
        }
    }

    private static final class RecipeMethods {
        final Method matches;
        final Method result;
        final Method remaining;

        private RecipeMethods(
                @Nullable Method matches,
                @Nullable Method result,
                @Nullable Method remaining) {
            this.matches = matches;
            this.result = result;
            this.remaining = remaining;
        }
    }
}
