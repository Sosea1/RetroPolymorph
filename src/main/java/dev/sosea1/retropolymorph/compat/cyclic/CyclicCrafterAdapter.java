package dev.sosea1.retropolymorph.compat.cyclic;

import dev.sosea1.retropolymorph.api.MachineRecipeAdapter;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/** Machine-API adapter for Cyclic's Auto-Crafter ("Авто-верстак"). */
public final class CyclicCrafterAdapter implements MachineRecipeAdapter {

    public static final CyclicCrafterAdapter INSTANCE = new CyclicCrafterAdapter();

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final String CONTAINER_NAME = "com.lothrazar.cyclicmagic.block.crafter.ContainerCrafter";
    private static final String TILE_NAME = "com.lothrazar.cyclicmagic.block.crafter.TileEntityCrafter";

    private static final Field TILE_FIELD;
    private static boolean loggedExtractionFailure = false;

    static {
        Field found = null;
        try {
            Class<?> clazz = Class.forName(CONTAINER_NAME);
            for (Class<?> current = clazz; current != null && current != Container.class; current = current.getSuperclass()) {
                try {
                    found = current.getDeclaredField("tile");
                    found.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (ReflectiveOperationException | SecurityException | LinkageError ignored) {
        }
        TILE_FIELD = found;
    }

    private CyclicCrafterAdapter() {
    }

    @Override
    public boolean recognizes(Container container) {
        return container != null && CONTAINER_NAME.equals(container.getClass().getName());
    }

    @Override
    @Nullable
    public MachineRecipeSurface openSurface(Container container) {
        if (!recognizes(container)) {
            return null;
        }

        TileEntity tile = extractTile(container);
        if (tile == null || !TILE_NAME.equals(tile.getClass().getName())) {
            return null;
        }

        if (container.inventorySlots.size() < 19) {
            return null;
        }

        return new CyclicCrafterSurface(container, tile);
    }

    @Nullable
    private static TileEntity extractTile(Container container) {
        if (TILE_FIELD != null) {
            try {
                Object value = TILE_FIELD.get(container);
                return value instanceof TileEntity ? (TileEntity) value : null;
            } catch (ReflectiveOperationException | SecurityException exception) {
                if (!loggedExtractionFailure) {
                    loggedExtractionFailure = true;
                    LOGGER.warn("Failed to extract tile from ContainerCrafter reflectively", exception);
                }
            }
        }
        return null;
    }
}
