package dev.sosea1.retropolymorph.core;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Failure-isolated recipe operations used both during candidate discovery and
 * within CraftingManager resolution.
 *
 * <p>Third-party 1.12.2 recipes occasionally assume a very specific container,
 * player, or world state and can throw from {@link IRecipe#matches},
 * {@link IRecipe#getCraftingResult}, or {@link IRecipe#getRemainingItems}. A broken
 * candidate must not take down the server thread or cause item duplication.
 * Failing probes are safely caught, logged once per recipe class, and fallen
 * back to safe defaults.</p>
 */
public final class RecipeProbe {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final AtomicLong FAILURES = new AtomicLong();
    private static final ConcurrentMap<String, Boolean> REPORTED =
            new ConcurrentHashMap<String, Boolean>();
    private static final ConcurrentMap<String, Boolean> BROKEN_CLASSES =
            new ConcurrentHashMap<String, Boolean>();

    private RecipeProbe() {
    }

    public static InventoryCrafting sanitizeMatrix(@Nullable InventoryCrafting matrix) {
        if (matrix == null || matrix instanceof SanitizedCraftingMatrix) {
            return matrix;
        }
        int maxGridSlots = matrix.getWidth() * matrix.getHeight();
        if (matrix.getSizeInventory() > maxGridSlots) {
            return new SanitizedCraftingMatrix(matrix);
        }
        return matrix;
    }

    public static boolean matches(IRecipe recipe, InventoryCrafting matrix, World world) {
        if (recipe == null || matrix == null || world == null) {
            return false;
        }
        try {
            return recipe.matches(sanitizeMatrix(matrix), world);
        } catch (RuntimeException | LinkageError exception) {
            report("matches", recipe, exception);
            return false;
        }
    }

    public static ItemStack craftingResult(IRecipe recipe, InventoryCrafting matrix) {
        if (recipe == null || matrix == null) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack result = recipe.getCraftingResult(sanitizeMatrix(matrix));
            return result == null ? ItemStack.EMPTY : result;
        } catch (RuntimeException | LinkageError exception) {
            report("getCraftingResult", recipe, exception);
            return ItemStack.EMPTY;
        }
    }

    public static NonNullList<ItemStack> remainingItems(IRecipe recipe, InventoryCrafting matrix) {
        if (matrix == null) {
            return NonNullList.create();
        }
        InventoryCrafting clean = sanitizeMatrix(matrix);
        int size = clean.getSizeInventory();
        if (recipe == null) {
            return NonNullList.withSize(size, ItemStack.EMPTY);
        }
        try {
            NonNullList<ItemStack> remainders = recipe.getRemainingItems(clean);
            if (remainders == null || remainders.size() != size) {
                if (remainders != null) {
                    reportRemainderSizeMismatch(recipe, remainders.size(), size);
                }
                return NonNullList.withSize(size, ItemStack.EMPTY);
            }
            return remainders;
        } catch (RuntimeException | LinkageError exception) {
            reportRemainderFailure(recipe, exception);
            try {
                NonNullList<ItemStack> forgeDefault = ForgeHooks.defaultRecipeGetRemainingItems(clean);
                if (forgeDefault != null && forgeDefault.size() == size) {
                    return forgeDefault;
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Ignore secondary fallback failure
            }
            return NonNullList.withSize(size, ItemStack.EMPTY);
        }
    }

    public static long getFailureCount() {
        return FAILURES.get();
    }

    public static int getReportedRecipeClassCount() {
        return BROKEN_CLASSES.size();
    }

    public static void resetDiagnostics() {
        FAILURES.set(0L);
        REPORTED.clear();
        BROKEN_CLASSES.clear();
    }

    public static void reportRemainderFailure(IRecipe recipe, Throwable exception) {
        report("getRemainingItems", recipe, exception);
    }

    private static void reportRemainderSizeMismatch(IRecipe recipe, int actualSize, int expectedSize) {
        FAILURES.incrementAndGet();
        String className = recipe == null ? "<null>" : recipe.getClass().getName();
        BROKEN_CLASSES.putIfAbsent(className, Boolean.TRUE);
        String key = "getRemainingItemsSize|" + className;
        if (REPORTED.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        ResourceLocation id = null;
        try {
            id = recipe == null ? null : recipe.getRegistryName();
        } catch (RuntimeException | LinkageError ignored) {
            // Diagnostics must never turn a probe failure into a second failure.
        }

        LOGGER.warn(
                "Recipe getRemainingItems returned {} items for matrix size {} on {} ({}); normalizing to empty remainder list to prevent dupe or crash.",
                Integer.valueOf(actualSize),
                Integer.valueOf(expectedSize),
                id == null ? "<unregistered>" : id,
                className);
    }

    private static void report(String operation, IRecipe recipe, Throwable exception) {
        FAILURES.incrementAndGet();
        String className = recipe == null ? "<null>" : recipe.getClass().getName();
        BROKEN_CLASSES.putIfAbsent(className, Boolean.TRUE);
        String key = operation + "|" + className;
        if (REPORTED.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        ResourceLocation id = null;
        try {
            id = recipe == null ? null : recipe.getRegistryName();
        } catch (RuntimeException | LinkageError ignored) {
            // Diagnostics must never turn a probe failure into a second failure.
        }

        LOGGER.warn(
                "Recipe probe {} failed for {} ({}); RetroPolymorph will skip this candidate. Cause: {}",
                operation,
                id == null ? "<unregistered>" : id,
                className,
                exception == null ? "<unknown>" : exception.toString());
        LOGGER.debug("Recipe probe failure details", exception);
    }
}
