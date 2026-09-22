package dev.sosea1.retropolymorph.compat;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import net.minecraft.inventory.Container;

/**
 * Safety-only guard for whole integration families that should never fall back
 * to generic crafting/furnace detection.
 */
public final class PackagePrefixGuardAdapter implements RecipeSelectionAdapter {

    private final String packagePrefix;

    public PackagePrefixGuardAdapter(String packagePrefix) {
        if (packagePrefix == null || packagePrefix.isEmpty()) {
            throw new IllegalArgumentException("packagePrefix");
        }
        this.packagePrefix = packagePrefix.endsWith(".") ? packagePrefix : packagePrefix + ".";
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (container != null && hasPrefixInHierarchy(container.getClass(), this.packagePrefix)) {
            return AdapterDetectionResult.blockFallback();
        }
        return AdapterDetectionResult.miss();
    }


    private static boolean hasPrefixInHierarchy(Class<?> type, String prefix) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Package currentPackage = current.getPackage();
            String packageName = currentPackage == null ? "" : currentPackage.getName();
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
