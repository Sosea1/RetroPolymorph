package com.sosea1.retropolymorph.compat.artisanworktables;

import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.api.RecipeOption;
import com.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import com.sosea1.retropolymorph.core.CraftingMatrixExtension;
import com.sosea1.retropolymorph.core.RecipeSelectionState;
import com.sosea1.retropolymorph.core.TransientSelectionStore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ArtisanWorktableReflection {

    private static final String CONTAINER =
            "com.codetaylor.mc.artisanworktables.modules.worktables.gui.AWContainer";
    private static final String RESULT_SLOT =
            "com.codetaylor.mc.artisanworktables.modules.worktables.gui.slot.CraftingResultSlot";
    private static final String VANILLA_CACHE =
            "com.codetaylor.mc.artisanworktables.modules.worktables.recipe.VanillaRecipeCache";
    private static final String CUSTOM_PREFIX = "artisan:";

    private static final TransientSelectionStore CUSTOM_SELECTIONS = new TransientSelectionStore();
    private static volatile Field vanillaLastRecipeField;

    private ArtisanWorktableReflection() {
    }

    static boolean isContainer(@Nullable Container container) {
        return container != null && hasClassInHierarchy(container.getClass(), CONTAINER);
    }

    @Nullable
    static Binding bind(Container container) {
        if (!isContainer(container)) {
            return null;
        }
        try {
            Object tile = invoke(container, "getTile");
            Object playerValue = readField(container, "player");
            Object matrixValue = invoke(tile, "getCraftingMatrixHandler");
            Object vanillaValue = invoke(tile, "getInventoryWrapper");
            Slot result = findResultSlot(container);
            if (!(playerValue instanceof EntityPlayer)
                    || !(matrixValue instanceof IItemHandler)
                    || !(vanillaValue instanceof InventoryCrafting)
                    || result == null) {
                return null;
            }
            return new Binding(
                    container,
                    tile,
                    (EntityPlayer) playerValue,
                    (IItemHandler) matrixValue,
                    (InventoryCrafting) vanillaValue,
                    result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("bind", exception);
            return null;
        }
    }

    static List<RecipeOption> findCustomOptions(Binding binding) {
        try {
            Object registry = invoke(binding.tile, "getWorktableRecipeRegistry");
            Object tier = invoke(binding.tile, "getTier");
            List<?> candidates = recipesForTier(registry, tier);
            if (candidates.isEmpty()) {
                return Collections.emptyList();
            }

            Object craftingContext = invoke(binding.tile, "getCraftingContext", binding.player);
            ArrayList<RecipeOption> options = new ArrayList<RecipeOption>();
            for (int index = candidates.size() - 1; index >= 0; index--) {
                Object recipe = candidates.get(index);
                if (recipe == null || probeNativeRecipe(binding, registry, recipe) != recipe) {
                    continue;
                }
                String key = keyForRecipe(recipe);
                ItemStack output = outputOf(recipe, craftingContext);
                if (RecipeKey.isWireSafe(key) && output != null && !output.isEmpty()) {
                    options.add(new RecipeOption(key, output));
                }
            }
            return options;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("findOptions", exception);
            return Collections.emptyList();
        }
    }

    static boolean hasAnyCustomMatch(Binding binding) {
        try {
            Object registry = invoke(binding.tile, "getWorktableRecipeRegistry");
            Object tier = invoke(binding.tile, "getTier");
            List<?> candidates = recipesForTier(registry, tier);
            for (int index = candidates.size() - 1; index >= 0; index--) {
                Object recipe = candidates.get(index);
                if (recipe != null && probeNativeRecipe(binding, registry, recipe) == recipe) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("hasCustomMatch", exception);
        }
        return false;
    }

    static boolean selectCustom(Binding binding, String recipeKey) {
        String recipeName = customRecipeName(recipeKey);
        if (recipeName == null) {
            return false;
        }
        try {
            Object registry = invoke(binding.tile, "getWorktableRecipeRegistry");
            Object recipe = invoke(registry, "getRecipe", recipeName);
            if (recipe == null || probeNativeRecipe(binding, registry, recipe) != recipe) {
                return false;
            }
            CUSTOM_SELECTIONS.set(binding.tile, binding.player.getUniqueID(), recipeKey);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("selectCustom", exception);
            return false;
        }
    }

    static void clearCustom(Binding binding) {
        CUSTOM_SELECTIONS.clear(binding.tile, binding.player.getUniqueID());
    }

    @Nullable
    static String getSelectedCustomKey(Binding binding) {
        return CUSTOM_SELECTIONS.get(binding.tile, binding.player.getUniqueID());
    }

    static boolean isCustomKey(@Nullable String recipeKey) {
        return customRecipeName(recipeKey) != null;
    }

    static void setRemoteCustom(Binding binding, String recipeKey) {
        if (customRecipeName(recipeKey) != null) {
            CUSTOM_SELECTIONS.set(binding.tile, binding.player.getUniqueID(), recipeKey);
        }
    }

    public static void beginSelectedRecipe(Object tile, EntityPlayer player) {
        if (tile == null || player == null || ArtisanRecipeSelectionBridge.isProbeActive()) {
            return;
        }
        if (ArtisanRecipeSelectionBridge.isActive()) {
            ArtisanRecipeSelectionBridge.endSelection();
        }
        UUID playerId = player.getUniqueID();
        String recipeName = customRecipeName(CUSTOM_SELECTIONS.get(tile, playerId));
        if (recipeName == null) {
            invalidateVanillaCacheWhenSelected(tile);
            return;
        }
        try {
            Object registry = invoke(tile, "getWorktableRecipeRegistry");
            Object recipe = invoke(registry, "getRecipe", recipeName);
            if (recipe == null) {
                CUSTOM_SELECTIONS.clear(tile, playerId);
                return;
            }
            ArtisanRecipeSelectionBridge.beginSelection(recipe, tile, playerId);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            CUSTOM_SELECTIONS.clear(tile, playerId);
            report("beginSelectedRecipe", exception);
        }
    }


    private static void invalidateVanillaCacheWhenSelected(Object tile) {
        try {
            Object wrapper = invoke(tile, "getInventoryWrapper");
            if (!(wrapper instanceof CraftingMatrixExtension)) {
                return;
            }
            RecipeSelectionState state = ((CraftingMatrixExtension) wrapper)
                    .retropolymorph$peekRecipeSelectionState();
            if (state != null && state.hasSelection()) {
                invalidateVanillaRecipeCache();
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("invalidateSelectedVanillaCache", exception);
        }
    }

    public static void finishSelectedRecipe(Object tile, EntityPlayer player, @Nullable Object resolved) {
        if (tile == null || player == null) {
            return;
        }
        UUID playerId = player.getUniqueID();
        if (!ArtisanRecipeSelectionBridge.isSelection(tile, playerId)) {
            return;
        }
        Object forced = ArtisanRecipeSelectionBridge.forcedRecipe();
        if (forced != resolved) {
            CUSTOM_SELECTIONS.clear(tile, playerId);
        }
        ArtisanRecipeSelectionBridge.endSelection();
    }

    static boolean allowsVanillaCrafting(Object tile) {
        try {
            return Boolean.TRUE.equals(invoke(tile, "allowVanillaCrafting"));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("allowVanillaCrafting", exception);
            return false;
        }
    }

    static void refresh(Container container) {
        try {
            invoke(container, "updateRecipeOutput");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("refresh", exception);
        }
    }

    static int clientStateToken(Binding binding) {
        int hash = 1;
        try {
            hash = 31 * hash + stackHandlerHash(invoke(binding.tile, "getToolHandler"));
            hash = 31 * hash + stackHandlerHash(invokeOptional(binding.tile, "getSecondaryIngredientHandler"));
            Object tank = invoke(binding.tile, "getTank");
            hash = 31 * hash + fluidHash(tank == null ? null : invoke(tank, "getFluid"));
            Object tier = invoke(binding.tile, "getTier");
            hash = 31 * hash + (tier == null ? 0 : tier.hashCode());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("clientStateToken", exception);
        }
        hash = 31 * hash + playerExperienceTotal(binding.player);
        hash = 31 * hash + (binding.player.isCreative() ? 1 : 0);
        String selected = CUSTOM_SELECTIONS.get(binding.tile, binding.player.getUniqueID());
        return 31 * hash + (selected == null ? 0 : selected.hashCode());
    }

    public static void invalidateVanillaRecipeCache() {
        try {
            Field field = vanillaLastRecipeField;
            if (field == null) {
                synchronized (ArtisanWorktableReflection.class) {
                    field = vanillaLastRecipeField;
                    if (field == null) {
                        Class<?> cache = Class.forName(VANILLA_CACHE);
                        field = cache.getDeclaredField("LAST_RECIPE");
                        field.setAccessible(true);
                        vanillaLastRecipeField = field;
                    }
                }
            }
            Object value = field.get(null);
            if (value instanceof ThreadLocal) {
                Object cached = ((ThreadLocal<?>) value).get();
                if (cached instanceof Object[] && ((Object[]) cached).length > 0) {
                    ((Object[]) cached)[0] = null;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            report("invalidateVanillaCache", exception);
        }
    }

    private static Object probeNativeRecipe(Binding binding, Object registry, Object recipe)
            throws ReflectiveOperationException {
        List<?> nativeList = nativeRecipeList(registry);
        if (nativeList == null) {
            return null;
        }
        synchronized (nativeList) {
            ArrayList<Object> snapshot = new ArrayList<Object>(nativeList.size());
            snapshot.addAll(nativeList);
            ArtisanRecipeSelectionBridge.beginProbe(recipe);
            try {
                return invoke(binding.tile, "getRecipe", binding.player);
            } finally {
                ArtisanRecipeSelectionBridge.endProbe();
                @SuppressWarnings("unchecked")
                List<Object> mutable = (List<Object>) nativeList;
                mutable.clear();
                mutable.addAll(snapshot);
            }
        }
    }

    @Nullable
    private static List<?> nativeRecipeList(Object registry) throws IllegalAccessException {
        Object value = readField(registry, "recipeList");
        return value instanceof List ? (List<?>) value : null;
    }

    private static List<?> recipesForTier(Object registry, Object tier)
            throws ReflectiveOperationException {
        ArrayList<Object> recipes = new ArrayList<Object>();
        Object value = invoke(registry, "getRecipeListByTier", tier, recipes);
        return value instanceof List ? (List<?>) value : recipes;
    }

    @Nullable
    private static ItemStack outputOf(Object recipe, Object craftingContext)
            throws ReflectiveOperationException {
        Object artisanStack = invoke(recipe, "getBaseOutput", craftingContext);
        Object output = artisanStack == null ? null : invoke(artisanStack, "toItemStack");
        return output instanceof ItemStack ? (ItemStack) output : null;
    }

    @Nullable
    private static String keyForRecipe(Object recipe) throws ReflectiveOperationException {
        Object name = invoke(recipe, "getName");
        return name instanceof String && !((String) name).isEmpty()
                ? CUSTOM_PREFIX + name
                : null;
    }

    @Nullable
    private static String customRecipeName(@Nullable String key) {
        if (!RecipeKey.isWireSafe(key) || !key.startsWith(CUSTOM_PREFIX)) {
            return null;
        }
        String name = key.substring(CUSTOM_PREFIX.length());
        return name.isEmpty() ? null : name;
    }

    @Nullable
    private static Slot findResultSlot(Container container) {
        Slot result = null;
        for (Slot slot : container.inventorySlots) {
            if (!hasClassInHierarchy(slot.getClass(), RESULT_SLOT)) {
                continue;
            }
            if (result != null && result != slot) {
                return null;
            }
            result = slot;
        }
        return result;
    }

    private static int stackHandlerHash(@Nullable Object value) {
        if (!(value instanceof IItemHandler)) {
            return 0;
        }
        IItemHandler handler = (IItemHandler) value;
        int hash = 1;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                hash = 31 * hash;
                continue;
            }
            int stackHash = stack.getItem().hashCode();
            stackHash = 31 * stackHash + stack.getMetadata();
            stackHash = 31 * stackHash + stack.getCount();
            NBTTagCompound tag = stack.getTagCompound();
            hash = 31 * hash + 31 * stackHash + (tag == null ? 0 : tag.hashCode());
        }
        return hash;
    }

    private static int fluidHash(@Nullable Object value) {
        if (!(value instanceof FluidStack)) {
            return 0;
        }
        FluidStack fluid = (FluidStack) value;
        int hash = fluid.getFluid() == null ? 0 : fluid.getFluid().getName().hashCode();
        hash = 31 * hash + fluid.amount;
        return 31 * hash + (fluid.tag == null ? 0 : fluid.tag.hashCode());
    }

    private static int playerExperienceTotal(EntityPlayer player) {
        int level = player.experienceLevel;
        int fromLevel;
        if (level <= 15) {
            fromLevel = level * (2 * 7 + (level - 1) * 2) / 2;
        } else if (level <= 30) {
            int n = level - 15;
            fromLevel = 315 + n * (2 * 37 + (n - 1) * 5) / 2;
        } else {
            int n = level - 30;
            fromLevel = 1395 + n * (2 * 112 + (n - 1) * 9) / 2;
        }
        int cap = level >= 30 ? 112 + (level - 30) * 9
                : level >= 15 ? 37 + (level - 15) * 5
                : 7 + level * 2;
        return (int) (fromLevel + player.experience * cap);
    }

    @Nullable
    private static Object invokeOptional(Object owner, String name)
            throws ReflectiveOperationException {
        Method method = findMethod(owner.getClass(), name, 0);
        return method == null ? null : method.invoke(owner);
    }

    private static Object invoke(Object owner, String name, Object... arguments)
            throws ReflectiveOperationException {
        if (owner == null) {
            return null;
        }
        Method method = findCompatibleMethod(owner.getClass(), name, arguments);
        if (method == null) {
            throw new NoSuchMethodException(owner.getClass().getName() + "." + name);
        }
        return method.invoke(owner, arguments);
    }

    @Nullable
    private static Object readField(Object owner, String name) throws IllegalAccessException {
        Field field = findField(owner.getClass(), name);
        return field == null ? null : field.get(owner);
    }

    @Nullable
    private static Method findCompatibleMethod(Class<?> type, String name, Object[] arguments) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != arguments.length) {
                    continue;
                }
                Class<?>[] parameters = method.getParameterTypes();
                boolean compatible = true;
                for (int index = 0; index < parameters.length; index++) {
                    Object argument = arguments[index];
                    if (argument != null && !boxed(parameters[index]).isInstance(argument)) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name)
                        && method.getParameterTypes().length == parameterCount) {
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

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static boolean hasClassInHierarchy(Class<?> type, String className) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (className.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void report(String operation, Throwable exception) {
        IntegrationHealthRegistry.recordBindingFailure(
                "artisanworktables", operation, exception);
    }

    static final class Binding {
        final Container container;
        final Object tile;
        final EntityPlayer player;
        final IItemHandler matrixHandler;
        final InventoryCrafting vanillaMatrix;
        final Slot resultSlot;

        private Binding(
                Container container,
                Object tile,
                EntityPlayer player,
                IItemHandler matrixHandler,
                InventoryCrafting vanillaMatrix,
                Slot resultSlot) {
            this.container = container;
            this.tile = tile;
            this.player = player;
            this.matrixHandler = matrixHandler;
            this.vanillaMatrix = vanillaMatrix;
            this.resultSlot = resultSlot;
        }
    }
}
