package dev.sosea1.retropolymorph.compat.thermal;

import net.minecraft.item.crafting.IRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Calls Thermal's own public CrafterRecipe.validRecipe(IRecipe) predicate.
 * Keeping the rule in Thermal avoids copying its machine-specific recipe policy.
 */
public final class ThermalSequentialFabricatorRecipeSupport {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final String RECIPE_WRAPPER_CLASS =
            "cofh.thermalexpansion.block.machine.TileCrafter$CrafterRecipe";

    private static volatile boolean resolved;
    private static volatile Method validRecipeMethod;

    private ThermalSequentialFabricatorRecipeSupport() {
    }

    public static boolean isSupported(IRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        Method method = resolve();
        if (method == null) {
            return false;
        }
        try {
            Object result = method.invoke(null, recipe);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.debug(
                    "Thermal Sequential Fabricator recipe-support probe failed for {}",
                    recipe.getRegistryName(),
                    exception);
            return false;
        }
    }

    private static Method resolve() {
        if (resolved) {
            return validRecipeMethod;
        }
        synchronized (ThermalSequentialFabricatorRecipeSupport.class) {
            if (resolved) {
                return validRecipeMethod;
            }
            try {
                Class<?> wrapper = Class.forName(
                        RECIPE_WRAPPER_CLASS,
                        false,
                        ThermalSequentialFabricatorRecipeSupport.class.getClassLoader());
                Method method = wrapper.getMethod("validRecipe", IRecipe.class);
                method.setAccessible(true);
                validRecipeMethod = method;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                LOGGER.debug(
                        "Thermal Sequential Fabricator recipe policy is unavailable; "
                                + "focused selector will stay disabled for this surface",
                        exception);
                validRecipeMethod = null;
            } finally {
                resolved = true;
            }
            return validRecipeMethod;
        }
    }
}
