package dev.sosea1.retropolymorph.compat.thermal;

import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** Persistent selected Forge recipe for one Thermal Sequential Fabricator. */
public final class ThermalSequentialFabricatorSelectionStore {

    private static final String TAG = "retropolymorphThermalCrafterRecipe";

    private ThermalSequentialFabricatorSelectionStore() {
    }

    @Nullable
    public static ResourceLocation getSelectedRecipeId(@Nullable TileEntity tile) {
        if (tile == null) {
            return null;
        }
        String value = tile.getTileData().getString(TAG);
        return RecipeKey.isWireSafe(value) ? RecipeKey.parseForgeId(value) : null;
    }

    public static void writeSelectedRecipeId(
            @Nullable TileEntity tile,
            @Nullable ResourceLocation recipeId) {
        if (tile == null) {
            return;
        }
        NBTTagCompound data = tile.getTileData();
        if (recipeId == null) {
            if (!data.hasKey(TAG)) {
                return;
            }
            data.removeTag(TAG);
        } else {
            String next = recipeId.toString();
            if (data.hasKey(TAG) && next.equals(data.getString(TAG))) {
                return;
            }
            data.setString(TAG, next);
        }
        tile.markDirty();
    }
}
