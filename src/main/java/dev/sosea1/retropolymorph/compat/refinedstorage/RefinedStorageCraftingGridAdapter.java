package dev.sosea1.retropolymorph.compat.refinedstorage;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.CraftingContextDetector;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nullable;

/** Focused integration for the Refined Storage 1.12 Crafting Grid. */
public final class RefinedStorageCraftingGridAdapter implements RecipeSelectionAdapter {

    public static final RefinedStorageCraftingGridAdapter INSTANCE =
            new RefinedStorageCraftingGridAdapter();

    private RefinedStorageCraftingGridAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!(container instanceof RefinedStorageCraftingGridAccess)) {
            return AdapterDetectionResult.miss();
        }

        RefinedStorageCraftingGridAccess access =
                (RefinedStorageCraftingGridAccess) container;
        if (!access.retropolymorph$isRefinedStorageCraftingGrid()) {
            return AdapterDetectionResult.blockFallback();
        }

        InventoryCrafting matrix =
                access.retropolymorph$getRefinedStorageCraftingMatrix();
        if (matrix == null) {
            return AdapterDetectionResult.blockFallback();
        }

        RecipeSelectionContext detected = CraftingContextDetector.detect(container);
        if (detected == null || detected.getRecipeMatrix() != matrix) {
            return AdapterDetectionResult.blockFallback();
        }

        return AdapterDetectionResult.match(new RefinedStorageCraftingGridContext(
                container,
                access,
                matrix,
                detected.getResultSlot()));
    }
}
