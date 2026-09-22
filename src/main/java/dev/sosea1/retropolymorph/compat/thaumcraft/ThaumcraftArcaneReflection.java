package dev.sosea1.retropolymorph.compat.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Optional-class reflection kept out of the normal class linkage path. */
final class ThaumcraftArcaneReflection {

    private static final String CONTAINER = "thaumcraft.common.container.ContainerArcaneWorkbench";
    private static final String ARCANE_RECIPE = "thaumcraft.api.crafting.IArcaneRecipe";
    private static final String CAPABILITIES = "thaumcraft.api.capabilities.ThaumcraftCapabilities";
    private static final String PLAYER_KNOWLEDGE = "thaumcraft.api.capabilities.IPlayerKnowledge";

    private static final ClassValue<Field> TILE_FIELDS = new ClassValue<Field>() {
        @Override
        protected Field computeValue(Class<?> type) {
            return findField(type, "tileEntity");
        }
    };

    private static final ClassValue<Field> PLAYER_FIELDS = new ClassValue<Field>() {
        @Override
        protected Field computeValue(Class<?> type) {
            return findField(type, "ip");
        }
    };

    private static final ClassValue<Field> MATRIX_FIELDS = new ClassValue<Field>() {
        @Override
        protected Field computeValue(Class<?> type) {
            return findField(type, "inventoryCraft");
        }
    };

    private static volatile Class<?> arcaneRecipeClass;
    private static volatile Method getResearchMethod;
    private static volatile Method getKnowledgeMethod;
    private static volatile Method isResearchKnownMethod;
    private static volatile boolean apiLookupAttempted;
    private static volatile int indexedRegistrySize = -1;
    private static volatile List<IRecipe> arcaneRecipes = Collections.emptyList();

    private ThaumcraftArcaneReflection() {
    }

    static boolean isArcaneWorkbench(Container container) {
        return container != null && hasClassInHierarchy(container.getClass(), CONTAINER);
    }

    @Nullable
    static InventoryCrafting getMatrix(Container container) {
        Object tile = getFieldValue(TILE_FIELDS.get(container.getClass()), container);
        if (tile == null) {
            return null;
        }
        Field matrixField = MATRIX_FIELDS.get(tile.getClass());
        Object value = getFieldValue(matrixField, tile);
        return value instanceof InventoryCrafting ? (InventoryCrafting) value : null;
    }

    @Nullable
    static EntityPlayer getPlayer(Container container) {
        Object value = getFieldValue(PLAYER_FIELDS.get(container.getClass()), container);
        return value instanceof InventoryPlayer ? ((InventoryPlayer) value).player : null;
    }

    static boolean isArcaneRecipe(IRecipe recipe) {
        Class<?> type = getArcaneRecipeClass();
        return type != null && recipe != null && type.isInstance(recipe);
    }


    static List<IRecipe> getArcaneRecipes() {
        int registrySize = ForgeRegistries.RECIPES.getValuesCollection().size();
        List<IRecipe> snapshot = arcaneRecipes;
        if (registrySize == indexedRegistrySize) {
            return snapshot;
        }
        synchronized (ThaumcraftArcaneReflection.class) {
            if (registrySize == indexedRegistrySize) {
                return arcaneRecipes;
            }
            ArrayList<IRecipe> found = new ArrayList<IRecipe>();
            for (IRecipe recipe : ForgeRegistries.RECIPES) {
                if (isArcaneRecipe(recipe)) {
                    found.add(recipe);
                }
            }
            arcaneRecipes = found.isEmpty()
                    ? Collections.<IRecipe>emptyList()
                    : Collections.unmodifiableList(found);
            indexedRegistrySize = registrySize;
            return arcaneRecipes;
        }
    }

    static boolean isRecipeSelectable(IRecipe recipe, EntityPlayer player) {
        return isResearchKnown(recipe, player) && isVanillaRecipeSelectable(recipe, player);
    }

    static boolean isVanillaRecipeSelectable(IRecipe recipe, EntityPlayer player) {
        if (recipe == null || player == null) {
            return false;
        }
        if (player.world == null
                || player.world.isRemote
                || !player.world.getGameRules().getBoolean("doLimitedCrafting")
                || recipe.isDynamic()) {
            return true;
        }
        return player instanceof EntityPlayerMP
                && ((EntityPlayerMP) player).getRecipeBook().isUnlocked(recipe);
    }

    static boolean isResearchKnown(IRecipe recipe, EntityPlayer player) {
        if (recipe == null || player == null || !ensureApiMethods()) {
            return false;
        }
        try {
            String research = (String) getResearchMethod.invoke(recipe);
            Object knowledge = getKnowledgeMethod.invoke(null, player);
            if (knowledge == null) {
                return false;
            }
            Object value = isResearchKnownMethod.invoke(
                    knowledge, research == null ? "" : research);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException | LinkageError exception) {
            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure("thaumcraft", "hasRequiredResearch", exception);
            return false;
        }
    }

    @Nullable
    private static Class<?> getArcaneRecipeClass() {
        ensureApiMethods();
        return arcaneRecipeClass;
    }

    private static boolean ensureApiMethods() {
        if (apiLookupAttempted) {
            return arcaneRecipeClass != null
                    && getResearchMethod != null
                    && getKnowledgeMethod != null
                    && isResearchKnownMethod != null;
        }
        synchronized (ThaumcraftArcaneReflection.class) {
            if (apiLookupAttempted) {
                return arcaneRecipeClass != null
                        && getResearchMethod != null
                        && getKnowledgeMethod != null
                        && isResearchKnownMethod != null;
            }
            try {
                ClassLoader loader = ThaumcraftArcaneReflection.class.getClassLoader();
                Class<?> recipe = Class.forName(ARCANE_RECIPE, false, loader);
                Class<?> capabilities = Class.forName(CAPABILITIES, false, loader);
                Class<?> playerKnowledge = Class.forName(PLAYER_KNOWLEDGE, false, loader);
                arcaneRecipeClass = recipe;
                getResearchMethod = recipe.getMethod("getResearch");
                getKnowledgeMethod = capabilities.getMethod("getKnowledge", EntityPlayer.class);
                isResearchKnownMethod = playerKnowledge.getMethod("isResearchKnown", String.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure("thaumcraft", "ensureApiMethods", ignored);
                arcaneRecipeClass = null;
                getResearchMethod = null;
                getKnowledgeMethod = null;
                isResearchKnownMethod = null;
            }
            apiLookupAttempted = true;
        }
        return arcaneRecipeClass != null
                && getResearchMethod != null
                && getKnowledgeMethod != null
                && isResearchKnownMethod != null;
    }

    @Nullable
    private static Object getFieldValue(@Nullable Field field, Object owner) {
        if (field == null || owner == null) {
            return null;
        }
        try {
            return field.get(owner);
        } catch (IllegalAccessException | LinkageError exception) {
            return null;
        }
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
