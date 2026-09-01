package dev.sosea1.retropolymorph.diagnostic;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapters;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.CraftingContext;
import dev.sosea1.retropolymorph.mixin.SlotCraftingAccessor;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/** Explicit, cold-path compatibility diagnostics. Never used by normal GUI detection. */
public final class CraftingDetectionDiagnostics {

    private CraftingDetectionDiagnostics() {
    }

    public static Report inspect(Container container) {
        int slotCraftingCount = 0;
        int accessorCount = 0;
        int resultCount = 0;
        Slot exactResult = null;
        InventoryCrafting exactMatrix = null;
        Slot topologyResult = null;
        boolean exactAmbiguous = false;
        IdentityHashMap<InventoryCrafting, Integer> matrixReferences =
                new IdentityHashMap<InventoryCrafting, Integer>();

        for (Slot slot : container.inventorySlots) {
            if (slot instanceof SlotCrafting) {
                slotCraftingCount++;
                if (slot instanceof SlotCraftingAccessor) {
                    accessorCount++;
                    InventoryCrafting matrix = ((SlotCraftingAccessor) slot).retropolymorph$getCraftMatrix();
                    if (exactResult == null) {
                        exactResult = slot;
                        exactMatrix = matrix;
                    } else if (exactMatrix != matrix) {
                        exactAmbiguous = true;
                    }
                }
            }

            if (slot.inventory instanceof InventoryCrafting) {
                InventoryCrafting matrix = (InventoryCrafting) slot.inventory;
                Integer count = matrixReferences.get(matrix);
                matrixReferences.put(matrix, count == null ? 1 : count + 1);
            }

            if (slot.inventory instanceof InventoryCraftResult) {
                resultCount++;
                if (topologyResult == null) {
                    topologyResult = slot;
                }
            }
        }

        SelectionContext adapted = RecipeSelectionAdapters.detect(container);
        if (adapted != null) {
            return report(
                    container,
                    Route.FOCUSED_ADAPTER,
                    adapted,
                    slotCraftingCount,
                    accessorCount,
                    matrixReferences.size(),
                    resultCount,
                    "focused adapter accepted this container before generic detection");
        }

        if (exactResult != null && !exactAmbiguous) {
            return report(
                    container,
                    Route.EXACT_SLOT_CRAFTING,
                    new CraftingContext(container, exactMatrix, exactResult),
                    slotCraftingCount,
                    accessorCount,
                    matrixReferences.size(),
                    resultCount,
                    "SlotCrafting exposes one unambiguous craftMatrix");
        }

        if (exactAmbiguous) {
            return unsupported(
                    container,
                    slotCraftingCount,
                    accessorCount,
                    matrixReferences.size(),
                    resultCount,
                    "multiple SlotCrafting outputs reference different crafting matrices");
        }

        InventoryCrafting bestMatrix = null;
        int bestReferences = 0;
        boolean topologyAmbiguous = false;
        for (Map.Entry<InventoryCrafting, Integer> entry : matrixReferences.entrySet()) {
            int references = entry.getValue();
            if (references > bestReferences) {
                bestReferences = references;
                bestMatrix = entry.getKey();
                topologyAmbiguous = false;
            } else if (references == bestReferences) {
                topologyAmbiguous = true;
            }
        }

        if (resultCount == 1 && bestMatrix != null && !topologyAmbiguous) {
            return report(
                    container,
                    Route.INVENTORY_TOPOLOGY,
                    new CraftingContext(container, bestMatrix, topologyResult),
                    slotCraftingCount,
                    accessorCount,
                    matrixReferences.size(),
                    resultCount,
                    "one result inventory and one strongest InventoryCrafting candidate");
        }

        String detail;
        if (resultCount > 1) {
            detail = "multiple InventoryCraftResult-backed output slots are present";
        } else if (resultCount == 0 && slotCraftingCount > 0 && accessorCount == 0) {
            detail = "SlotCrafting exists but its craftMatrix accessor is unavailable";
        } else if (resultCount == 0) {
            detail = "no InventoryCraftResult-backed output slot was found";
        } else if (matrixReferences.isEmpty()) {
            detail = "no InventoryCrafting-backed input slots were found";
        } else if (topologyAmbiguous) {
            detail = "multiple InventoryCrafting candidates have the same topology score";
        } else {
            detail = "container does not match a supported generic crafting topology";
        }

        return unsupported(
                container,
                slotCraftingCount,
                accessorCount,
                matrixReferences.size(),
                resultCount,
                detail);
    }

    private static Report unsupported(
            Container container,
            int slotCraftingCount,
            int accessorCount,
            int matrixCount,
            int resultCount,
            String detail) {
        return report(
                container,
                Route.UNSUPPORTED,
                null,
                slotCraftingCount,
                accessorCount,
                matrixCount,
                resultCount,
                detail);
    }

    private static Report report(
            Container container,
            Route route,
            @Nullable SelectionContext context,
            int slotCraftingCount,
            int accessorCount,
            int matrixCount,
            int resultCount,
            String detail) {
        return new Report(
                container.getClass().getName(),
                route,
                context,
                container.inventorySlots.size(),
                slotCraftingCount,
                accessorCount,
                matrixCount,
                resultCount,
                detail);
    }

    public enum Route {
        FOCUSED_ADAPTER,
        EXACT_SLOT_CRAFTING,
        INVENTORY_TOPOLOGY,
        UNSUPPORTED
    }

    /** Immutable command-facing snapshot; public fields keep this debug-only DTO intentionally small. */
    public static final class Report {

        public final String containerClass;
        public final Route route;
        @Nullable
        public final SelectionContext context;
        public final int totalSlots;
        public final int slotCraftingCount;
        public final int slotCraftingAccessorCount;
        public final int craftingMatrixCount;
        public final int resultInventoryCount;
        public final String detail;

        private Report(
                String containerClass,
                Route route,
                @Nullable SelectionContext context,
                int totalSlots,
                int slotCraftingCount,
                int slotCraftingAccessorCount,
                int craftingMatrixCount,
                int resultInventoryCount,
                String detail) {
            this.containerClass = containerClass;
            this.route = route;
            this.context = context;
            this.totalSlots = totalSlots;
            this.slotCraftingCount = slotCraftingCount;
            this.slotCraftingAccessorCount = slotCraftingAccessorCount;
            this.craftingMatrixCount = craftingMatrixCount;
            this.resultInventoryCount = resultInventoryCount;
            this.detail = detail;
        }
    }
}
