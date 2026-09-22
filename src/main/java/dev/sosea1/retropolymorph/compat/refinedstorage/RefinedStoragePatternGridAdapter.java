package dev.sosea1.retropolymorph.compat.refinedstorage;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.CraftingContextDetector;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nullable;

/** Regular (non-processing) Refined Storage 1.12 Pattern Grid integration. */
public final class RefinedStoragePatternGridAdapter implements RecipeSelectionAdapter {

    public static final RefinedStoragePatternGridAdapter INSTANCE =
            new RefinedStoragePatternGridAdapter();

    private RefinedStoragePatternGridAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!(container instanceof RefinedStorageCraftingGridAccess)) {
            return AdapterDetectionResult.miss();
        }

        RefinedStorageCraftingGridAccess access =
                (RefinedStorageCraftingGridAccess) container;
        if (!access.retropolymorph$isRefinedStoragePatternGrid()
                || access.retropolymorph$isRefinedStorageProcessingPattern()) {
            return AdapterDetectionResult.blockFallback();
        }

        InventoryCrafting matrix = access.retropolymorph$getRefinedStorageCraftingMatrix();
        if (matrix == null) {
            return AdapterDetectionResult.blockFallback();
        }

        RecipeSelectionContext detected = CraftingContextDetector.detect(container);
        if (detected == null || detected.getRecipeMatrix() != matrix) {
            return AdapterDetectionResult.blockFallback();
        }

        return AdapterDetectionResult.match(new RefinedStoragePatternGridContext(
                container,
                access,
                matrix,
                detected.getResultSlot()));
    }
}
