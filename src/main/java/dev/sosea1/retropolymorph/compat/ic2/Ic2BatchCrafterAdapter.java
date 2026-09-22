package dev.sosea1.retropolymorph.compat.ic2;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.mixin.compat.ic2.Ic2BatchCrafterAccess;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Detects IC2's Batch Crafter without linking IC2 classes into the base mod. */
public final class Ic2BatchCrafterAdapter implements RecipeSelectionAdapter {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    public static final Ic2BatchCrafterAdapter INSTANCE = new Ic2BatchCrafterAdapter();
    private static final String CONTAINER_CLASS =
            "ic2.core.block.machine.container.ContainerBatchCrafter";

    private Ic2BatchCrafterAdapter() {
    }

    @Override
    public dev.sosea1.retropolymorph.api.AdapterDetectionResult probe(Container container) {
        if (!CONTAINER_CLASS.equals(container.getClass().getName())) {
            return dev.sosea1.retropolymorph.api.AdapterDetectionResult.miss();
        }

        RecipeSelectionContext context = createContext(container);
        return context != null
                ? dev.sosea1.retropolymorph.api.AdapterDetectionResult.match(context)
                : dev.sosea1.retropolymorph.api.AdapterDetectionResult.blockFallback();
    }

    @Nullable
    private RecipeSelectionContext createContext(Container container) {
        if (!CONTAINER_CLASS.equals(container.getClass().getName())) {
            return null;
        }

        Ic2BatchCrafterAccess access = findAccess(container);
        if (access == null) {
            LOGGER.debug(
                    "IC2 Batch Crafter probe unsupported: container={}, reason=no_tile_access, slots={}",
                    container.getClass().getName(),
                    Integer.valueOf(container.inventorySlots.size()));
            return null;
        }

        InventoryCrafting matrix = access.retropolymorph$getBatchCraftingMatrix();
        if (matrix == null) {
            LOGGER.debug(
                    "IC2 Batch Crafter probe unsupported: container={}, reason=no_crafting_matrix",
                    container.getClass().getName());
            return null;
        }

        Slot resultSlot = findResultSlot(container, matrix, access);
        if (resultSlot == null) {
            LOGGER.debug(
                    "IC2 Batch Crafter probe unsupported: container={}, reason=no_result_slot, slots={}, matrixSlots={}",
                    container.getClass().getName(),
                    Integer.valueOf(container.inventorySlots.size()),
                    Integer.valueOf(countMatrixSlots(container, matrix)));
            return null;
        }

        int[] anchor = findButtonAnchor(container, matrix, resultSlot);
        LOGGER.debug(
                "IC2 Batch Crafter probe supported: resultListIndex={}, resultSlotIndex={}, x={}, y={}, matrixSlots={}, anchorX={}, anchorY={}",
                Integer.valueOf(container.inventorySlots.indexOf(resultSlot)),
                Integer.valueOf(resultSlot.getSlotIndex()),
                Integer.valueOf(resultSlot.xPos),
                Integer.valueOf(resultSlot.yPos),
                Integer.valueOf(countMatrixSlots(container, matrix)),
                Integer.valueOf(anchor[0]),
                Integer.valueOf(anchor[1]));
        return new Ic2BatchCrafterContext(container, access, resultSlot, anchor[0], anchor[1]);
    }

    /**
     * IC2 does not expose the Batch Crafter tile through a stable Forge-facing
     * container API. First use slot inventories (cheap/common), then inspect the
     * concrete IC2 container fields once at GUI/context creation time. This is a
     * cold compatibility probe, never part of recipe matching or a render tick.
     */
    @Nullable
    private static Ic2BatchCrafterAccess findAccess(Container container) {
        for (Slot slot : container.inventorySlots) {
            IInventory inventory = slot.inventory;
            if (inventory instanceof Ic2BatchCrafterAccess) {
                return (Ic2BatchCrafterAccess) inventory;
            }
        }

        Class<?> type = container.getClass();
        while (type != null && type != Object.class) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (LinkageError | RuntimeException exception) {
                break;
            }

            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(container);
                    if (value instanceof Ic2BatchCrafterAccess) {
                        return (Ic2BatchCrafterAccess) value;
                    }
                } catch (IllegalAccessException | LinkageError | RuntimeException ignored) {
                    // Try the next field. Failure here must only disable this adapter.
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    /**
     * Do not assume a fixed list index for IC2 slots. When the Batch Crafter exposes
     * its 3x3 recipe matrix directly as InventoryCrafting slots, the preview/result
     * anchor is selected from the nearest non-matrix machine slot to the right of
     * the matrix centre. If another transformer wraps those slots, the fallback
     * below keeps recipe semantics on the real tile matrix and only relaxes the
     * visual anchor lookup.
     */
    @Nullable
    private static Slot findResultSlot(
            Container container,
            InventoryCrafting matrix,
            Ic2BatchCrafterAccess access) {
        int matrixCount = 0;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        Slot matrixFallback = null;
        long matrixFallbackScore = Long.MAX_VALUE;

        for (Slot slot : container.inventorySlots) {
            if (slot.inventory != matrix) {
                continue;
            }
            matrixCount++;
            minX = Math.min(minX, slot.xPos);
            maxX = Math.max(maxX, slot.xPos);
            minY = Math.min(minY, slot.yPos);
            maxY = Math.max(maxY, slot.yPos);
        }

        if (matrixCount == 0) {
            // Some IC2/container transformers wrap the hologram matrix slots in
            // another inventory object. The selected-recipe logic still uses
            // the tile's real InventoryCrafting, so keep the integration alive
            // and use a machine slot only as the selector anchor.
            for (Slot slot : container.inventorySlots) {
                Object inventory = slot.inventory;
                if (inventory == access || inventory instanceof Ic2BatchCrafterAccess) {
                    return slot;
                }
            }
            return container.inventorySlots.isEmpty() ? null : container.inventorySlots.get(0);
        }

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory != matrix) {
                continue;
            }
            long centerScore = Math.abs(slot.xPos - centerX) + Math.abs(slot.yPos - centerY);
            if (centerScore < matrixFallbackScore) {
                matrixFallbackScore = centerScore;
                matrixFallback = slot;
            }
        }

        Slot bestMachineSlot = null;
        long bestMachineScore = Long.MAX_VALUE;
        Slot bestAnySlot = null;
        long bestAnyScore = Long.MAX_VALUE;

        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == matrix || slot.xPos <= maxX) {
                continue;
            }

            int dx = slot.xPos - maxX;
            int dy = Math.abs(slot.yPos - centerY);

            // Result preview is on the same row as the matrix centre. Reject
            // distant player inventory/upgrade rows before scoring candidates.
            if (dy > 20) {
                continue;
            }

            long score = (long) dx * 4L + (long) dy * 16L;
            if (score < bestAnyScore) {
                bestAnyScore = score;
                bestAnySlot = slot;
            }

            Object inventory = slot.inventory;
            if ((inventory == access || inventory instanceof Ic2BatchCrafterAccess)
                    && score < bestMachineScore) {
                bestMachineScore = score;
                bestMachineSlot = slot;
            }
        }

        if (bestMachineSlot != null) {
            return bestMachineSlot;
        }
        if (bestAnySlot != null) {
            return bestAnySlot;
        }

        // The slot is only an anchor for the selector button; recipe execution
        // is owned by the tile accessor above. If a heavily transformed IC2 GUI
        // hides its preview slot, anchoring above the matrix is still preferable
        // to disabling conflict selection entirely.
        return matrixFallback;
    }


    /**
     * IC2 renders the Batch Crafter preview as machine GUI state rather than a
     * normal result Slot in some builds/transformer combinations. Derive a
     * stable visual anchor from the upper non-player 3x3 grid when possible.
     */
    private static int[] findButtonAnchor(
            Container container,
            InventoryCrafting matrix,
            Slot fallback) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int count = 0;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == matrix) {
                minX = Math.min(minX, slot.xPos);
                maxX = Math.max(maxX, slot.xPos);
                minY = Math.min(minY, slot.yPos);
                maxY = Math.max(maxY, slot.yPos);
                count++;
            }
        }
        if (count >= 9) {
            return new int[] { maxX + 28, (minY + maxY) / 2 };
        }

        // Wrapped ghost slots: look for a geometric 3x3 among machine slots only.
        for (Slot topLeft : container.inventorySlots) {
            if (topLeft.inventory instanceof InventoryPlayer) {
                continue;
            }
            int x = topLeft.xPos;
            int y = topLeft.yPos;
            if (hasMachineSlot(container, x + 18, y)
                    && hasMachineSlot(container, x + 36, y)
                    && hasMachineSlot(container, x, y + 18)
                    && hasMachineSlot(container, x + 18, y + 18)
                    && hasMachineSlot(container, x + 36, y + 18)
                    && hasMachineSlot(container, x, y + 36)
                    && hasMachineSlot(container, x + 18, y + 36)
                    && hasMachineSlot(container, x + 36, y + 36)) {
                return new int[] { x + 64, y + 18 };
            }
        }
        return new int[] { fallback.xPos, fallback.yPos };
    }

    private static boolean hasMachineSlot(Container container, int x, int y) {
        for (Slot slot : container.inventorySlots) {
            if (!(slot.inventory instanceof InventoryPlayer) && slot.xPos == x && slot.yPos == y) {
                return true;
            }
        }
        return false;
    }

    private static int countMatrixSlots(Container container, InventoryCrafting matrix) {
        int count = 0;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == matrix) {
                count++;
            }
        }
        return count;
    }
}
