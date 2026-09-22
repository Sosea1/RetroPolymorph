package dev.sosea1.retropolymorph.compat.extrautils2;

import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bridges RetroPolymorph's tile-owned XU2 selection into XU2's real native recipe scan.
 *
 * <p>Extra Utilities 2's Mechanical and Analog Crafters do not resolve their recipe
 * through {@code CraftingManager.findMatchingRecipe}. Both load an {@code XUCrafter},
 * obtain {@code CraftingHelper112.getRecipeList()}, and manually choose the first
 * matching recipe. The selected recipe therefore has to participate in that exact
 * ordered scan instead of being injected into a CraftingManager path XU2 never calls.</p>
 *
 * <p>This helper returns a zero-copy list view with the selected recipe swapped into
 * index 0. XU2 still runs its own {@code matches}, result, matcher/cache and operation
 * logic unchanged. The native XUCrafter matrix is also seeded so Analog Crafter's
 * later CompatHelper/CraftingManager remainder lookup stays on the same recipe.</p>
 */
public final class ExtraUtilities2NativeRecipeBridge {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    private static final String MECHANICAL = "com.rwtema.extrautils2.tile.TileCrafter";
    private static final String ANALOG = "com.rwtema.extrautils2.tile.TileAnalogCrafter";

    private static final AtomicLong SCAN_HITS = new AtomicLong();
    private static final AtomicLong SELECTED_SCAN_HITS = new AtomicLong();
    private static final AtomicLong REORDERS = new AtomicLong();
    private static final AtomicLong MECHANICAL_REORDERS = new AtomicLong();
    private static final AtomicLong ANALOG_REORDERS = new AtomicLong();
    private static final AtomicLong NATIVE_MATRIX_SEEDS = new AtomicLong();
    private static final AtomicLong MISSING_SELECTED = new AtomicLong();
    private static final AtomicBoolean MECHANICAL_PROOF_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean ANALOG_PROOF_LOGGED = new AtomicBoolean();

    private static final Map<Class<?>, Field> CRAFTER_FIELDS =
            new ConcurrentHashMap<Class<?>, Field>();
    private static final Map<Class<?>, Boolean> NO_CRAFTER_FIELD =
            new ConcurrentHashMap<Class<?>, Boolean>();

    private ExtraUtilities2NativeRecipeBridge() {
    }

    public static List<IRecipe> selectedFirst(TileEntity tile, List<IRecipe> recipes) {
        SCAN_HITS.incrementAndGet();
        if (!PolymorphConfig.isIntegrationExtraUtilities2Enabled()
                || tile == null
                || recipes == null
                || recipes.isEmpty()
                || !(tile instanceof ExtraUtilities2SelectionAccess)) {
            return recipes == null ? Collections.<IRecipe>emptyList() : recipes;
        }

        ExtraUtilities2SelectionAccess access = (ExtraUtilities2SelectionAccess) tile;
        ResourceLocation selectedId = access.retropolymorph$getExtraUtilities2Recipe();
        seedNativeCrafter(tile, selectedId);
        if (selectedId == null) {
            return recipes;
        }

        SELECTED_SCAN_HITS.incrementAndGet();
        IRecipe selected = ForgeRegistries.RECIPES.getValue(selectedId);
        if (selected == null) {
            MISSING_SELECTED.incrementAndGet();
            access.retropolymorph$setExtraUtilities2Recipe(null);
            seedNativeCrafter(tile, null);
            return recipes;
        }

        int selectedIndex = indexOf(recipes, selected, selectedId);
        if (selectedIndex < 0) {
            // XU2 cannot execute a Forge recipe which is not present in the list it
            // actually scans. Fail open to native ordering and drop the stale id.
            MISSING_SELECTED.incrementAndGet();
            access.retropolymorph$setExtraUtilities2Recipe(null);
            seedNativeCrafter(tile, null);
            return recipes;
        }
        if (selectedIndex == 0) {
            return recipes;
        }

        REORDERS.incrementAndGet();
        String tileName = tile.getClass().getName();
        if (MECHANICAL.equals(tileName)) {
            MECHANICAL_REORDERS.incrementAndGet();
            if (MECHANICAL_PROOF_LOGGED.compareAndSet(false, true)) {
                LOGGER.debug(
                        "XU2 native recipe bridge active: machine=MechanicalCrafter, selected={}, originalIndex={}",
                        selectedId,
                        Integer.valueOf(selectedIndex));
            }
        } else if (ANALOG.equals(tileName)) {
            ANALOG_REORDERS.incrementAndGet();
            if (ANALOG_PROOF_LOGGED.compareAndSet(false, true)) {
                LOGGER.debug(
                        "XU2 native recipe bridge active: machine=AnalogCrafter, selected={}, originalIndex={}",
                        selectedId,
                        Integer.valueOf(selectedIndex));
            }
        }
        return new SelectedFirstList(recipes, selectedIndex);
    }

    private static int indexOf(List<IRecipe> recipes, IRecipe selected, ResourceLocation selectedId) {
        for (int i = 0; i < recipes.size(); i++) {
            IRecipe candidate = recipes.get(i);
            if (candidate == selected) {
                return i;
            }
            if (candidate != null) {
                ResourceLocation candidateId = ForgeRegistries.RECIPES.getKey(candidate);
                if (selectedId.equals(candidateId)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void seedNativeCrafter(TileEntity tile, @Nullable ResourceLocation selectedId) {
        Field field = getCrafterField(tile.getClass());
        if (field == null) {
            return;
        }
        Object value;
        try {
            value = field.get(tile);
        } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
            return;
        }
        if (!(value instanceof InventoryCrafting)) {
            return;
        }

        InventoryCrafting matrix = (InventoryCrafting) value;
        if (selectedId == null) {
            RecipeSelectionSeeder.clear(matrix);
        } else {
            RecipeSelectionSeeder.seed(matrix, selectedId);
            NATIVE_MATRIX_SEEDS.incrementAndGet();
        }
    }

    @Nullable
    private static Field getCrafterField(Class<?> tileClass) {
        Field cached = CRAFTER_FIELDS.get(tileClass);
        if (cached != null) {
            return cached;
        }
        if (NO_CRAFTER_FIELD.containsKey(tileClass)) {
            return null;
        }

        Class<?> type = tileClass;
        while (type != null) {
            try {
                Field field = type.getDeclaredField("crafter");
                field.setAccessible(true);
                CRAFTER_FIELDS.put(tileClass, field);
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (RuntimeException | LinkageError ignored) {
                break;
            }
        }
        NO_CRAFTER_FIELD.put(tileClass, Boolean.TRUE);
        return null;
    }

    public static long getScanHits() {
        return SCAN_HITS.get();
    }

    public static long getSelectedScanHits() {
        return SELECTED_SCAN_HITS.get();
    }

    public static long getReorders() {
        return REORDERS.get();
    }

    public static long getMechanicalReorders() {
        return MECHANICAL_REORDERS.get();
    }

    public static long getAnalogReorders() {
        return ANALOG_REORDERS.get();
    }

    public static long getNativeMatrixSeeds() {
        return NATIVE_MATRIX_SEEDS.get();
    }

    public static long getMissingSelected() {
        return MISSING_SELECTED.get();
    }

    private static final class SelectedFirstList extends AbstractList<IRecipe> {
        private final List<IRecipe> delegate;
        private final int selectedIndex;

        private SelectedFirstList(List<IRecipe> delegate, int selectedIndex) {
            this.delegate = delegate;
            this.selectedIndex = selectedIndex;
        }

        @Override
        public IRecipe get(int index) {
            if (index == 0) {
                return this.delegate.get(this.selectedIndex);
            }
            if (index == this.selectedIndex) {
                return this.delegate.get(0);
            }
            return this.delegate.get(index);
        }

        @Override
        public int size() {
            return this.delegate.size();
        }
    }
}
