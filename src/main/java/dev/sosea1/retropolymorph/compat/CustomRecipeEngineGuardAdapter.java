package dev.sosea1.retropolymorph.compat;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import net.minecraft.inventory.Container;

/**
 * Safety-only adapter for containers whose recipe engine must not be treated
 * as ordinary InventoryCrafting until a complete focused integration exists.
 */
public final class CustomRecipeEngineGuardAdapter implements RecipeSelectionAdapter {

    private final String targetClassName;

    public CustomRecipeEngineGuardAdapter(String targetClassName) {
        if (targetClassName == null || targetClassName.isEmpty()) {
            throw new IllegalArgumentException("targetClassName");
        }
        this.targetClassName = targetClassName;
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (container != null && hasClassInHierarchy(container.getClass(), this.targetClassName)) {
            return AdapterDetectionResult.blockFallback();
        }
        return AdapterDetectionResult.miss();
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
