package dev.sosea1.retropolymorph.compat.refinedstorage;

import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * RetroPolymorph metadata stored on Refined Storage crafting patterns.
 *
 * RS 1.12 stores the 3x3 ingredient exemplars but not the backing Forge recipe
 * id. That is ambiguous when multiple recipes match the same matrix. This tiny
 * namespaced compound pins the user's selected recipe without altering any RS
 * input/output fields.
 */
public final class RefinedStoragePatternData {

    private static final String ROOT = "RetroPolymorph";
    private static final String KEY_VERSION = "Version";
    private static final String KEY_RECIPE = "Recipe";
    private static final int FORMAT_VERSION = 1;

    // Refined Storage 1.6.x ItemPattern NBT. Kept here instead of importing RS
    // classes so the core compatibility layer remains optional at class-load time.
    private static final String RS_KEY_PROCESSING = "Processing";
    private static final String RS_INPUT_PREFIX = "Input_";

    private RefinedStoragePatternData() {
    }

    public static void writeSelectedRecipe(ItemStack pattern, @Nullable ResourceLocation recipeId) {
        if (pattern == null || pattern.isEmpty()) {
            return;
        }

        NBTTagCompound root = pattern.getTagCompound();
        if (root == null) {
            if (recipeId == null) {
                return;
            }
            root = new NBTTagCompound();
            pattern.setTagCompound(root);
        }

        if (recipeId == null) {
            root.removeTag(ROOT);
            return;
        }

        NBTTagCompound polymorph = new NBTTagCompound();
        polymorph.setInteger(KEY_VERSION, FORMAT_VERSION);
        polymorph.setString(KEY_RECIPE, recipeId.toString());
        root.setTag(ROOT, polymorph);
    }

    @Nullable
    public static ResourceLocation readSelectedRecipe(ItemStack pattern) {
        if (pattern == null || pattern.isEmpty() || !pattern.hasTagCompound()) {
            return null;
        }

        NBTTagCompound root = pattern.getTagCompound();
        if (root == null || !root.hasKey(ROOT, 10)) {
            return null;
        }

        NBTTagCompound polymorph = root.getCompoundTag(ROOT);
        if (polymorph.getInteger(KEY_VERSION) != FORMAT_VERSION) {
            return null;
        }

        String recipeKey = polymorph.getString(KEY_RECIPE);
        if (!RecipeKey.isWireSafe(recipeKey)) {
            return null;
        }
        try {
            return RecipeKey.parseForgeId(recipeKey);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    /** Returns true only for a regular (non-processing) RS pattern. */
    public static boolean isRegularCraftingPattern(ItemStack pattern) {
        if (pattern == null || pattern.isEmpty() || !pattern.hasTagCompound()) {
            return false;
        }
        NBTTagCompound root = pattern.getTagCompound();
        return root != null && !root.getBoolean(RS_KEY_PROCESSING);
    }

    /**
     * Checks whether RS has finished copying the pattern's saved 3x3 exemplars
     * into its live InventoryCrafting. NetworkNodeGrid 1.6.x copies one slot at
     * a time and fires onCraftingMatrixChanged after each slot, so this is the
     * safe point at which an encoded recipe id can be restored.
     */
    public static boolean matchesEncodedInputs(ItemStack pattern, InventoryCrafting matrix) {
        if (!isRegularCraftingPattern(pattern) || matrix == null || matrix.getSizeInventory() < 9) {
            return false;
        }

        NBTTagCompound root = pattern.getTagCompound();
        if (root == null) {
            return false;
        }

        for (int slot = 0; slot < 9; slot++) {
            String key = RS_INPUT_PREFIX + slot;
            ItemStack expected = root.hasKey(key, 10)
                    ? new ItemStack(root.getCompoundTag(key))
                    : ItemStack.EMPTY;
            ItemStack actual = matrix.getStackInSlot(slot);
            if (!ItemStack.areItemStacksEqual(expected, actual)) {
                return false;
            }
        }
        return true;
    }

}
