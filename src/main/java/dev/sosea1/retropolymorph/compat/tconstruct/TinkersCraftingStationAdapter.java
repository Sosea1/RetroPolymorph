package dev.sosea1.retropolymorph.compat.tconstruct;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.CraftingContextDetector;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/**
 * Focused compatibility route for Tinkers' Construct 1.12 Crafting Station.
 *
 * Tinkers intentionally keeps a real InventoryCraftingPersistent and its
 * SlotCraftingFastWorkbench extends vanilla SlotCrafting. We therefore reuse
 * the exact generic detector instead of touching Tinkers' private fields or
 * duplicating FastWorkbench semantics.
 */
public final class TinkersCraftingStationAdapter implements RecipeSelectionAdapter {

    public static final TinkersCraftingStationAdapter INSTANCE = new TinkersCraftingStationAdapter();

    private static final String TARGET =
            "slimeknights.tconstruct.tools.common.inventory.ContainerCraftingStation";

    private TinkersCraftingStationAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (container == null || !hasClassInHierarchy(container.getClass(), TARGET)) {
            return AdapterDetectionResult.miss();
        }
        SelectionContext detected = CraftingContextDetector.detect(container);
        if (detected instanceof dev.sosea1.retropolymorph.core.CraftingContext) {
            dev.sosea1.retropolymorph.core.CraftingContext crafting =
                    (dev.sosea1.retropolymorph.core.CraftingContext) detected;
            return AdapterDetectionResult.match(new TinkersCraftingStationContext(
                    container,
                    crafting.getRecipeMatrix(),
                    crafting.getResultSlot()));
        }
        return detected != null
                ? AdapterDetectionResult.match(detected)
                : AdapterDetectionResult.blockFallback();
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
