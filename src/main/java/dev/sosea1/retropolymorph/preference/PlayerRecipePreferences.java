package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

/**
 * Small bounded persistent preference store kept in Forge's player persisted NBT.
 * Entries are ordered by most recent explicit selection; the store never grows past 128 conflicts.
 */
public final class PlayerRecipePreferences {

    private static final String ROOT_TAG = "retropolymorphPreferences";
    private static final String ENTRIES_TAG = "entries";
    private static final String FINGERPRINT_TAG = "fingerprint";
    private static final String RECIPE_TAG = "recipe";
    private static final int MAX_ENTRIES = 128;
    private static final int MAX_FINGERPRINT_CHARS = 80;

    private PlayerRecipePreferences() {
    }

    @Nullable
    public static String lookup(@Nullable EntityPlayerMP player, @Nullable String fingerprint) {
        return player == null ? null : lookup(player.getEntityData(), fingerprint);
    }

    @Nullable
    public static String lookup(@Nullable NBTTagCompound entityData, @Nullable String fingerprint) {
        if (!isFingerprintSafe(fingerprint) || entityData == null) {
            return null;
        }

        NBTTagList entries = getEntries(entityData, false);
        if (entries == null) {
            return null;
        }

        int count = Math.min(entries.tagCount(), MAX_ENTRIES);
        for (int index = 0; index < count; index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            if (!fingerprint.equals(entry.getString(FINGERPRINT_TAG))) {
                continue;
            }

            String recipe = entry.getString(RECIPE_TAG);
            return RecipeKey.isWireSafe(recipe) ? recipe : null;
        }
        return null;
    }

    public static void remember(
            @Nullable EntityPlayerMP player,
            @Nullable String fingerprint,
            @Nullable String recipeKey) {
        if (player != null) {
            remember(player.getEntityData(), fingerprint, recipeKey);
        }
    }

    public static void remember(
            @Nullable NBTTagCompound entityData,
            @Nullable String fingerprint,
            @Nullable String recipeKey) {
        if (entityData == null || !isFingerprintSafe(fingerprint) || !RecipeKey.isWireSafe(recipeKey)) {
            return;
        }

        NBTTagList previous = getEntries(entityData, false);
        NBTTagList updated = new NBTTagList();
        updated.appendTag(createEntry(fingerprint, recipeKey));

        if (previous != null) {
            int count = Math.min(previous.tagCount(), MAX_ENTRIES);
            for (int index = 0; index < count && updated.tagCount() < MAX_ENTRIES; index++) {
                NBTTagCompound entry = previous.getCompoundTagAt(index);
                String oldFingerprint = entry.getString(FINGERPRINT_TAG);
                String oldRecipe = entry.getString(RECIPE_TAG);
                if (fingerprint.equals(oldFingerprint)
                        || !isFingerprintSafe(oldFingerprint)
                        || !RecipeKey.isWireSafe(oldRecipe)) {
                    continue;
                }
                updated.appendTag(createEntry(oldFingerprint, oldRecipe));
            }
        }

        NBTTagCompound root = getRoot(entityData, true);
        if (root != null) {
            root.setTag(ENTRIES_TAG, updated);
        }
    }

    public static void forget(@Nullable EntityPlayerMP player, @Nullable String fingerprint) {
        if (player != null) {
            forget(player.getEntityData(), fingerprint);
        }
    }

    public static void forget(@Nullable NBTTagCompound entityData, @Nullable String fingerprint) {
        if (entityData == null || !isFingerprintSafe(fingerprint)) {
            return;
        }

        NBTTagList previous = getEntries(entityData, false);
        if (previous == null) {
            return;
        }

        NBTTagList updated = new NBTTagList();
        int count = Math.min(previous.tagCount(), MAX_ENTRIES);
        for (int index = 0; index < count; index++) {
            NBTTagCompound entry = previous.getCompoundTagAt(index);
            String oldFingerprint = entry.getString(FINGERPRINT_TAG);
            String oldRecipe = entry.getString(RECIPE_TAG);
            if (fingerprint.equals(oldFingerprint)
                    || !isFingerprintSafe(oldFingerprint)
                    || !RecipeKey.isWireSafe(oldRecipe)) {
                continue;
            }
            updated.appendTag(createEntry(oldFingerprint, oldRecipe));
        }
        NBTTagCompound root = getRoot(entityData, true);
        if (root != null) {
            root.setTag(ENTRIES_TAG, updated);
        }
    }

    public static int size(@Nullable EntityPlayerMP player) {
        return player == null ? 0 : size(player.getEntityData());
    }

    public static int size(@Nullable NBTTagCompound entityData) {
        if (entityData == null) {
            return 0;
        }
        NBTTagList entries = getEntries(entityData, false);
        return entries == null ? 0 : Math.min(entries.tagCount(), MAX_ENTRIES);
    }

    public static void clearAll(@Nullable EntityPlayerMP player) {
        if (player != null) {
            clearAll(player.getEntityData());
        }
    }

    public static void clearAll(@Nullable NBTTagCompound entityData) {
        if (entityData == null || !entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persisted.removeTag(ROOT_TAG);
    }

    private static NBTTagCompound createEntry(String fingerprint, String recipeKey) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString(FINGERPRINT_TAG, fingerprint);
        entry.setString(RECIPE_TAG, recipeKey);
        return entry;
    }

    @Nullable
    private static NBTTagList getEntries(NBTTagCompound entityData, boolean create) {
        NBTTagCompound root = getRoot(entityData, create);
        if (root == null) {
            return null;
        }
        if (!root.hasKey(ENTRIES_TAG, Constants.NBT.TAG_LIST)) {
            if (!create) {
                return null;
            }
            NBTTagList list = new NBTTagList();
            root.setTag(ENTRIES_TAG, list);
            return list;
        }
        return root.getTagList(ENTRIES_TAG, Constants.NBT.TAG_COMPOUND);
    }

    @Nullable
    private static NBTTagCompound getRoot(NBTTagCompound entityData, boolean create) {
        NBTTagCompound persisted;
        if (entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) {
            persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        } else if (create) {
            persisted = new NBTTagCompound();
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        } else {
            return null;
        }

        if (persisted.hasKey(ROOT_TAG, Constants.NBT.TAG_COMPOUND)) {
            return persisted.getCompoundTag(ROOT_TAG);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound root = new NBTTagCompound();
        persisted.setTag(ROOT_TAG, root);
        return root;
    }

    private static boolean isFingerprintSafe(@Nullable String fingerprint) {
        return fingerprint != null
                && !fingerprint.isEmpty()
                && fingerprint.length() <= MAX_FINGERPRINT_CHARS;
    }
}
