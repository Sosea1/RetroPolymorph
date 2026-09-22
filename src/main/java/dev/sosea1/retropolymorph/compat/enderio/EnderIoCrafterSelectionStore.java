package dev.sosea1.retropolymorph.compat.enderio;

import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** Persistent selected Forge recipe for one Ender IO Crafter tile. */
public final class EnderIoCrafterSelectionStore {

    private static final String TAG = "retropolymorphEnderIoCrafterRecipe";

    private EnderIoCrafterSelectionStore() {
    }

    @Nullable
    public static ResourceLocation getSelectedRecipeId(TileEntity tile) {
        if (tile == null) {
            return null;
        }
        String value = tile.getTileData().getString(TAG);
        return RecipeKey.isWireSafe(value) ? RecipeKey.parseForgeId(value) : null;
    }

    public static void setSelectedRecipeId(TileEntity tile, @Nullable ResourceLocation recipeId) {
        if (tile == null) {
            return;
        }
        NBTTagCompound data = tile.getTileData();
        if (recipeId == null) {
            data.removeTag(TAG);
        } else {
            data.setString(TAG, recipeId.toString());
        }
        tile.markDirty();
    }

    public static void clearIfSelected(TileEntity tile, @Nullable ResourceLocation expected) {
        if (tile == null || expected == null) {
            return;
        }
        ResourceLocation current = getSelectedRecipeId(tile);
        if (expected.equals(current)) {
            setSelectedRecipeId(tile, null);
        }
    }
}
