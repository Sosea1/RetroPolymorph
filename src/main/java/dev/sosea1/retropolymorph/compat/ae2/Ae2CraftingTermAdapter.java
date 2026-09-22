package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Dependency-free AE2 UEL / AE2WTLib crafting-terminal adapter.
 *
 * <p>The slot marker topology is the primary contract. Requiring a particular
 * container mixin was too strict for UEL forks and wireless wrappers: they can
 * keep the normal AE2 crafting slots while replacing the container class. Nine
 * AE2 crafting-matrix slots plus one AE2 crafting-output slot is specific enough
 * to identify the crafting surface without guessing arbitrary modded GUIs.</p>
 */
public final class Ae2CraftingTermAdapter implements RecipeSelectionAdapter {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    public static final Ae2CraftingTermAdapter INSTANCE = new Ae2CraftingTermAdapter();

    private Ae2CraftingTermAdapter() {
    }

    @Override
    public dev.sosea1.retropolymorph.api.AdapterDetectionResult probe(Container container) {
        if (container == null) {
            return dev.sosea1.retropolymorph.api.AdapterDetectionResult.miss();
        }
        RecipeSelectionContext context = createContext(container);
        if (context != null) {
            return dev.sosea1.retropolymorph.api.AdapterDetectionResult.match(context);
        }
        return WirelessTerminalSupport.isWirelessContainer(container)
                ? dev.sosea1.retropolymorph.api.AdapterDetectionResult.blockFallback()
                : dev.sosea1.retropolymorph.api.AdapterDetectionResult.miss();
    }

    @Nullable
    private RecipeSelectionContext createContext(Container container) {
        boolean wireless = WirelessTerminalSupport.isWirelessContainer(container);

        List<Slot> matrixSlots = new ArrayList<Slot>(9);
        Slot output = null;
        int resultCount = 0;

        for (Slot slot : container.inventorySlots) {
            if (slot instanceof Ae2CraftingMatrixSlot) {
                matrixSlots.add(slot);
            }
            if (slot instanceof Ae2CraftingResultSlot) {
                resultCount++;
                if (output == null) {
                    output = slot;
                }
            }
        }

        Slot[] inputs = normalizeMatrixSlots(matrixSlots);
        if (inputs == null || output == null || resultCount != 1) {
            if (wireless) {
                LOGGER.debug(
                        "Wireless AE2 selector probe unsupported: reason=slot_topology, matrixSlots={}, resultSlots={}, totalSlots={}, container={}",
                        Integer.valueOf(matrixSlots.size()),
                        Integer.valueOf(resultCount),
                        Integer.valueOf(container.inventorySlots.size()),
                        container.getClass().getName());
            }
            return null;
        }

        Ae2CraftingTermExtension extension = container instanceof Ae2CraftingTermExtension
                ? (Ae2CraftingTermExtension) container
                : null;
        int nativeMatrices = countNativeMatrices(inputs);

        LOGGER.debug(
                "AE2 selector probe supported: container={}, bridge={}, wireless={}, matrixSlots=9, outputSlot={}, nativeMatrices={}, totalSlots={}",
                container.getClass().getName(),
                Boolean.valueOf(extension != null),
                Boolean.valueOf(wireless),
                Integer.valueOf(container.inventorySlots.indexOf(output)),
                Integer.valueOf(nativeMatrices),
                Integer.valueOf(container.inventorySlots.size()));

        return new Ae2CraftingTermContext(
                container,
                extension,
                inputs,
                output,
                wireless);
    }

    @Nullable
    private static Slot[] normalizeMatrixSlots(List<Slot> slots) {
        if (slots.size() != 9) {
            return null;
        }

        List<Slot> sorted = new ArrayList<Slot>(slots);
        Collections.sort(sorted, new Comparator<Slot>() {
            @Override
            public int compare(Slot left, Slot right) {
                int byY = Integer.compare(left.yPos, right.yPos);
                return byY != 0 ? byY : Integer.compare(left.xPos, right.xPos);
            }
        });

        int minX = sorted.get(0).xPos;
        int minY = sorted.get(0).yPos;
        Slot[] result = new Slot[9];
        for (Slot slot : sorted) {
            int dx = slot.xPos - minX;
            int dy = slot.yPos - minY;
            if (dx < 0 || dy < 0 || dx % 18 != 0 || dy % 18 != 0) {
                return null;
            }
            int col = dx / 18;
            int row = dy / 18;
            if (col < 0 || col >= 3 || row < 0 || row >= 3) {
                return null;
            }
            int index = row * 3 + col;
            if (result[index] != null) {
                return null;
            }
            result[index] = slot;
        }
        for (Slot slot : result) {
            if (slot == null) {
                return null;
            }
        }
        return result;
    }

    private static int countNativeMatrices(Slot[] inputs) {
        List<InventoryCrafting> seen = new ArrayList<InventoryCrafting>(2);
        for (Slot slot : inputs) {
            if (slot.inventory instanceof InventoryCrafting) {
                InventoryCrafting matrix = (InventoryCrafting) slot.inventory;
                if (!seen.contains(matrix)) {
                    seen.add(matrix);
                }
            }
        }
        return seen.size();
    }
}
