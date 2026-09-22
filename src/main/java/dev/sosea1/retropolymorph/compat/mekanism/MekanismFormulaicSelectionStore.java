package dev.sosea1.retropolymorph.compat.mekanism;

import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** Persistent manual-mode recipe choice for one Formulaic Assemblicator tile. */
public final class MekanismFormulaicSelectionStore {

    private static final String TAG = "retropolymorphMekanismFormulaicRecipe";

    private MekanismFormulaicSelectionStore() {
    }

    @Nullable
    public static ResourceLocation getSelectedRecipeId(TileEntity tile) {
        if (tile == null) {
            return null;
        }
        String value = tile.getTileData().getString(TAG);
        return RecipeKey.isWireSafe(value) ? RecipeKey.parseForgeId(value) : null;
    }

    /**
     * Updates ForgeData without calling markDirty. The focused Mekanism mixin
     * invalidates cachedRecipe and then calls the tile's native markDirty(),
     * which both persists the tag and performs exactly one native recalc.
     */
    public static void writeSelectedRecipeId(TileEntity tile, @Nullable ResourceLocation recipeId) {
        if (tile == null) {
            return;
        }
        NBTTagCompound data = tile.getTileData();
        if (recipeId == null) {
            data.removeTag(TAG);
        } else {
            data.setString(TAG, recipeId.toString());
        }
    }
}
