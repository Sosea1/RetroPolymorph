package dev.sosea1.retropolymorph.compat.extrautils2;

import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

/** Shared recipe/output/remainder redirects used only from the two XU2 crafter mixins. */
public final class ExtraUtilities2RecipeRedirect {

    private static final AtomicLong LOOKUP_HITS = new AtomicLong();
    private static final AtomicLong SELECTED_HITS = new AtomicLong();
    private static final AtomicLong MECHANICAL_HITS = new AtomicLong();
    private static final AtomicLong ANALOG_HITS = new AtomicLong();

    private ExtraUtilities2RecipeRedirect() {
    }

    @Nullable
    public static IRecipe findMatchingRecipe(TileEntity tile, InventoryCrafting matrix, World world) {
        count(tile);
        IRecipe selected = resolveSelected(tile, matrix, world);
        return selected != null ? selected : CraftingManager.findMatchingRecipe(matrix, world);
    }

    public static ItemStack findMatchingResult(TileEntity tile, InventoryCrafting matrix, World world) {
        count(tile);
        IRecipe selected = resolveSelected(tile, matrix, world);
        return selected != null
                ? RecipeProbe.craftingResult(selected, matrix)
                : CraftingManager.findMatchingResult(matrix, world);
    }

    public static NonNullList<ItemStack> getRemainingItems(
            TileEntity tile,
            InventoryCrafting matrix,
            World world) {
        count(tile);
        IRecipe selected = resolveSelected(tile, matrix, world);
        if (selected != null) {
            try {
                return selected.getRemainingItems(matrix);
            } catch (RuntimeException | LinkageError exception) {
                RecipeProbe.reportRemainderFailure(selected, exception);
                // The selected recipe is known and matched. Returning an empty
                // list is safer than silently applying another recipe's
                // container-item semantics.
                NonNullList<ItemStack> empty = NonNullList.withSize(matrix.getSizeInventory(), ItemStack.EMPTY);
                return empty;
            }
        }
        return CraftingManager.getRemainingItems(matrix, world);
    }

    /**
     * Resolve the tile-owned recipe directly. Do not depend on a second global
     * CraftingManager interception layer: XU2's actual-operation redirect now
     * returns the selected recipe/result/remainders itself.
     */
    @Nullable
    private static IRecipe resolveSelected(TileEntity tile, InventoryCrafting matrix, World world) {
        if (!PolymorphConfig.isIntegrationExtraUtilities2Enabled()
                || !(tile instanceof ExtraUtilities2SelectionAccess)) {
            return null;
        }

        ExtraUtilities2SelectionAccess access = (ExtraUtilities2SelectionAccess) tile;
        ResourceLocation selected = access.retropolymorph$getExtraUtilities2Recipe();
        if (selected == null) {
            RecipeSelectionSeeder.clear(matrix);
            return null;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selected);
        if (recipe != null && RecipeProbe.matches(recipe, matrix, world)) {
            RecipeSelectionSeeder.seed(matrix, selected);
            SELECTED_HITS.incrementAndGet();
            return recipe;
        }

        access.retropolymorph$setExtraUtilities2Recipe(null);
        RecipeSelectionSeeder.clear(matrix);
        return null;
    }

    private static void count(TileEntity tile) {
        LOOKUP_HITS.incrementAndGet();
        String name = tile == null ? "" : tile.getClass().getName();
        if ("com.rwtema.extrautils2.tile.TileCrafter".equals(name)) {
            MECHANICAL_HITS.incrementAndGet();
        } else if ("com.rwtema.extrautils2.tile.TileAnalogCrafter".equals(name)) {
            ANALOG_HITS.incrementAndGet();
        }
    }

    public static long getLookupHits() {
        return LOOKUP_HITS.get();
    }

    public static long getSelectedHits() {
        return SELECTED_HITS.get();
    }

    public static long getMechanicalHits() {
        return MECHANICAL_HITS.get();
    }

    public static long getAnalogHits() {
        return ANALOG_HITS.get();
    }
}
