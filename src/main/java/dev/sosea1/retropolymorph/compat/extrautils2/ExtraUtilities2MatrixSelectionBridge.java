package dev.sosea1.retropolymorph.compat.extrautils2;

import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Last-resort, exact-family bridge for XU2 temporary crafting matrices.
 *
 * <p>Both XU2 crafters create/reuse vanilla {@link InventoryCrafting} instances,
 * but not every lookup happens through the same tile method in every 1.12 build.
 * InventoryCrafting still remembers its owning Container. For XU2-owned
 * containers/anonymous inner containers we can cheaply recover the exact tile
 * from the synthetic/declared field and seed the machine-owned recipe onto the
 * matrix before CraftingManager resolves it.</p>
 *
 * <p>The hot path is deliberately tiny for unrelated crafting: one null/state
 * check and a package-prefix test. Reflective field discovery is cached per XU2
 * owner class and never runs for normal crafting tables.</p>
 */
public final class ExtraUtilities2MatrixSelectionBridge {

    private static final String XU2_PREFIX = "com.rwtema.extrautils2.";
    private static final String CRAFTER_TILE = "com.rwtema.extrautils2.tile.TileCrafter";
    private static final String ANALOG_TILE = "com.rwtema.extrautils2.tile.TileAnalogCrafter";

    private static final ConcurrentMap<Class<?>, Field> OWNER_FIELDS =
            new ConcurrentHashMap<Class<?>, Field>();
    private static final Set<Class<?>> NO_OWNER_FIELD =
            Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    private static final AtomicLong OWNER_PROBES = new AtomicLong();
    private static final AtomicLong SEEDED = new AtomicLong();

    private ExtraUtilities2MatrixSelectionBridge() {
    }

    public static void seedIfOwned(InventoryCrafting matrix, World world) {
        if (!PolymorphConfig.isIntegrationExtraUtilities2Enabled() || matrix == null || world == null) {
            return;
        }

        Container owner = ((CraftingMatrixExtension) matrix).retropolymorph$getCraftingOwner();
        if (owner == null) {
            return;
        }
        String ownerClass = owner.getClass().getName();
        if (!ownerClass.startsWith(XU2_PREFIX)) {
            return;
        }

        OWNER_PROBES.incrementAndGet();
        ExtraUtilities2SelectionAccess access = findSelectionOwner(owner);
        if (!(access instanceof TileEntity)) {
            return;
        }

        ResourceLocation selected = access.retropolymorph$getExtraUtilities2Recipe();
        dev.sosea1.retropolymorph.core.RecipeSelectionState matrixState =
                ((CraftingMatrixExtension) matrix).retropolymorph$peekRecipeSelectionState();
        if (selected == null) {
            // XU2 commonly reuses temporary matrices. An old RP id must not
            // survive after the machine selection was cleared or changed.
            if (matrixState != null && matrixState.hasSelection()) {
                RecipeSelectionSeeder.clear(matrix);
            }
            return;
        }
        if (matrixState != null && selected.equals(matrixState.getSelectedRecipeId())) {
            return;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selected);
        if (recipe == null || !dev.sosea1.retropolymorph.core.RecipeProbe.matches(recipe, matrix, world)) {
            access.retropolymorph$setExtraUtilities2Recipe(null);
            return;
        }

        RecipeSelectionSeeder.seed(matrix, selected);
        SEEDED.incrementAndGet();
    }

    @Nullable
    private static ExtraUtilities2SelectionAccess findSelectionOwner(Container owner) {
        Class<?> type = owner.getClass();
        Field cached = OWNER_FIELDS.get(type);
        if (cached != null) {
            return read(cached, owner);
        }
        if (NO_OWNER_FIELD.contains(type)) {
            return null;
        }

        Field found = findOwnerField(type);
        if (found == null) {
            NO_OWNER_FIELD.add(type);
            return null;
        }
        OWNER_FIELDS.putIfAbsent(type, found);
        return read(found, owner);
    }

    @Nullable
    private static Field findOwnerField(Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            Field[] fields;
            try {
                fields = current.getDeclaredFields();
            } catch (RuntimeException | LinkageError exception) {
                return null;
            }
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String fieldType = field.getType().getName();
                if (!CRAFTER_TILE.equals(fieldType) && !ANALOG_TILE.equals(fieldType)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException ignored) {
                    // Continue probing. Optional compat must fail closed.
                }
            }
        }
        return null;
    }

    @Nullable
    private static ExtraUtilities2SelectionAccess read(Field field, Container owner) {
        try {
            Object value = field.get(owner);
            return value instanceof ExtraUtilities2SelectionAccess
                    ? (ExtraUtilities2SelectionAccess) value
                    : null;
        } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static long getOwnerProbeCount() {
        return OWNER_PROBES.get();
    }

    public static long getSeedCount() {
        return SEEDED.get();
    }
}
