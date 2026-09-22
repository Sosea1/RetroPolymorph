package dev.sosea1.retropolymorph.compat.extrautils2;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Class-specific topology resolver for Extra Utilities 2's DynamicContainer crafters.
 *
 * <p>This is deliberately not part of the generic crafting detector. XU2 does not
 * expose an InventoryCrafting/result pair through ordinary Container topology, but
 * both crafter GUIs do expose one visible 3x3 recipe grid. We only run this resolver
 * after the exact XU2 container class has matched.</p>
 */
final class ExtraUtilities2CrafterReflection {

    static final String CRAFTER_CONTAINER =
            "com.rwtema.extrautils2.tile.TileCrafter$CrafterContainer";
    static final String ANALOG_CONTAINER =
            "com.rwtema.extrautils2.tile.TileAnalogCrafter$ContainerAnalogCrafter";
    static final String CRAFTER_TILE = "com.rwtema.extrautils2.tile.TileCrafter";
    static final String ANALOG_TILE = "com.rwtema.extrautils2.tile.TileAnalogCrafter";

    private ExtraUtilities2CrafterReflection() {
    }

    static boolean recognizes(Container container) {
        if (container == null) {
            return false;
        }
        String name = container.getClass().getName();
        return CRAFTER_CONTAINER.equals(name) || ANALOG_CONTAINER.equals(name);
    }

    @Nullable
    static Resolved resolve(Container container) {
        if (!recognizes(container)) {
            return null;
        }

        TileEntity tile = findTile(container);
        if (!(tile instanceof ExtraUtilities2SelectionAccess)) {
            return null;
        }

        Slot[] inputSlots = findVisibleThreeByThree(container);
        if (inputSlots == null) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Slot slot : inputSlots) {
            minX = Math.min(minX, slot.xPos);
            minY = Math.min(minY, slot.yPos);
            maxX = Math.max(maxX, slot.xPos);
            maxY = Math.max(maxY, slot.yPos);
        }
        return new Resolved(tile, inputSlots, minX, minY, maxX, maxY);
    }

    private static volatile Field cachedCrafterTileField;
    private static volatile Field cachedAnalogTileField;

    @Nullable
    private static TileEntity findTile(Container container) {
        boolean isCrafter = CRAFTER_CONTAINER.equals(container.getClass().getName());
        String expected = isCrafter ? CRAFTER_TILE : ANALOG_TILE;
        Field cached = isCrafter ? cachedCrafterTileField : cachedAnalogTileField;
        if (cached != null) {
            try {
                Object value = cached.get(container);
                if (value instanceof TileEntity && expected.equals(value.getClass().getName())) {
                    return (TileEntity) value;
                }
            } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
            }
        }

        Class<?> type = container.getClass();
        while (type != null) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (RuntimeException | LinkageError exception) {
                dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure("extrautils2", "findTile", exception);
                return null;
            }
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(container);
                    if (value instanceof TileEntity && expected.equals(value.getClass().getName())) {
                        if (isCrafter) {
                            cachedCrafterTileField = field;
                        } else {
                            cachedAnalogTileField = field;
                        }
                        return (TileEntity) value;
                    }
                } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
                    // Try the next field. Optional compat must fail closed.
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Slot[] findVisibleThreeByThree(Container container) {
        List<Slot> machineSlots = new ArrayList<Slot>();
        for (Slot slot : container.inventorySlots) {
            if (slot == null || slot.inventory instanceof InventoryPlayer) {
                continue;
            }
            machineSlots.add(slot);
        }

        // Find exact 3x3 geometry with the normal 18px slot pitch. The two XU2
        // containers are already known here, so this is a focused layout bridge,
        // not a generic detector applied to arbitrary mod GUIs.
        Slot[] best = null;
        int bestScore = Integer.MAX_VALUE;
        for (Slot origin : machineSlots) {
            Slot[] candidate = new Slot[9];
            boolean complete = true;
            for (int row = 0; row < 3 && complete; row++) {
                for (int col = 0; col < 3; col++) {
                    Slot found = at(machineSlots, origin.xPos + col * 18, origin.yPos + row * 18);
                    if (found == null) {
                        complete = false;
                        break;
                    }
                    candidate[row * 3 + col] = found;
                }
            }
            if (!complete) {
                continue;
            }

            // Prefer the top-most compact 3x3 recipe area. This rejects player
            // inventory rows and the wide input/output buffers of Mechanical Crafter.
            int score = origin.yPos * 1024 + origin.xPos;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    @Nullable
    private static Slot at(List<Slot> slots, int x, int y) {
        for (Slot slot : slots) {
            if (slot.xPos == x && slot.yPos == y) {
                return slot;
            }
        }
        return null;
    }

    static final class Resolved {
        final TileEntity tile;
        final Slot[] inputSlots;
        final int minX;
        final int minY;
        final int maxX;
        final int maxY;

        Resolved(
                TileEntity tile,
                Slot[] inputSlots,
                int minX,
                int minY,
                int maxX,
                int maxY) {
            this.tile = tile;
            this.inputSlots = inputSlots;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }
}
