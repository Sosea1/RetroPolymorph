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
 *
 * <p>The canonical conflict fingerprint remains the durable semantic key. A
 * second bounded input-fingerprint index is maintained only so the server can
 * restore a known recipe before an asynchronous selector query has enumerated
 * every matching recipe again.</p>
 */
public final class PlayerRecipePreferences {

    private static final String ROOT_TAG = "retropolymorphPreferences";
    private static final String ENTRIES_TAG = "entries";
    private static final String INPUT_ENTRIES_TAG = "inputEntries";
    private static final String FINGERPRINT_TAG = "fingerprint";
    private static final String INPUT_FINGERPRINT_TAG = "input";
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
        return lookupEntry(entityData, ENTRIES_TAG, FINGERPRINT_TAG, fingerprint);
    }

    @Nullable
    public static String lookupInput(@Nullable EntityPlayerMP player, @Nullable String inputFingerprint) {
        return player == null ? null : lookupInput(player.getEntityData(), inputFingerprint);
    }

    @Nullable
    public static String lookupInput(@Nullable NBTTagCompound entityData, @Nullable String inputFingerprint) {
        return lookupEntry(entityData, INPUT_ENTRIES_TAG, INPUT_FINGERPRINT_TAG, inputFingerprint);
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
        rememberEntry(entityData, ENTRIES_TAG, FINGERPRINT_TAG, fingerprint, recipeKey);
    }

    public static void rememberInput(
            @Nullable EntityPlayerMP player,
            @Nullable String inputFingerprint,
            @Nullable String recipeKey) {
        if (player != null) {
            rememberInput(player.getEntityData(), inputFingerprint, recipeKey);
        }
    }

    public static void rememberInput(
            @Nullable NBTTagCompound entityData,
            @Nullable String inputFingerprint,
            @Nullable String recipeKey) {
        rememberEntry(entityData, INPUT_ENTRIES_TAG, INPUT_FINGERPRINT_TAG, inputFingerprint, recipeKey);
    }

    public static void forget(@Nullable EntityPlayerMP player, @Nullable String fingerprint) {
        if (player != null) {
            forget(player.getEntityData(), fingerprint);
        }
    }

    public static void forget(@Nullable NBTTagCompound entityData, @Nullable String fingerprint) {
        forgetEntry(entityData, ENTRIES_TAG, FINGERPRINT_TAG, fingerprint);
    }

    public static void forgetInput(@Nullable EntityPlayerMP player, @Nullable String inputFingerprint) {
        if (player != null) {
            forgetInput(player.getEntityData(), inputFingerprint);
        }
    }

    public static void forgetInput(@Nullable NBTTagCompound entityData, @Nullable String inputFingerprint) {
        forgetEntry(entityData, INPUT_ENTRIES_TAG, INPUT_FINGERPRINT_TAG, inputFingerprint);
    }

    public static int size(@Nullable EntityPlayerMP player) {
        return player == null ? 0 : size(player.getEntityData());
    }

    public static int size(@Nullable NBTTagCompound entityData) {
        return entryCount(entityData, ENTRIES_TAG);
    }

    public static int inputSize(@Nullable EntityPlayerMP player) {
        return player == null ? 0 : inputSize(player.getEntityData());
    }

    public static int inputSize(@Nullable NBTTagCompound entityData) {
        return entryCount(entityData, INPUT_ENTRIES_TAG);
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

    @Nullable
    private static String lookupEntry(
            @Nullable NBTTagCompound entityData,
            String listTag,
            String keyTag,
            @Nullable String key) {
        if (!isFingerprintSafe(key) || entityData == null) {
            return null;
        }

        NBTTagList entries = getEntries(entityData, listTag, false);
        if (entries == null) {
            return null;
        }

        int count = Math.min(entries.tagCount(), MAX_ENTRIES);
        for (int index = 0; index < count; index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            if (!key.equals(entry.getString(keyTag))) {
                continue;
            }

            String recipe = entry.getString(RECIPE_TAG);
            return RecipeKey.isWireSafe(recipe) ? recipe : null;
        }
        return null;
    }

    private static void rememberEntry(
            @Nullable NBTTagCompound entityData,
            String listTag,
            String keyTag,
            @Nullable String key,
            @Nullable String recipeKey) {
        if (entityData == null || !isFingerprintSafe(key) || !RecipeKey.isWireSafe(recipeKey)) {
            return;
        }

        NBTTagList previous = getEntries(entityData, listTag, false);
        NBTTagList updated = new NBTTagList();
        updated.appendTag(createEntry(keyTag, key, recipeKey));

        if (previous != null) {
            int count = Math.min(previous.tagCount(), MAX_ENTRIES);
            for (int index = 0; index < count && updated.tagCount() < MAX_ENTRIES; index++) {
                NBTTagCompound entry = previous.getCompoundTagAt(index);
                String oldKey = entry.getString(keyTag);
                String oldRecipe = entry.getString(RECIPE_TAG);
                if (key.equals(oldKey)
                        || !isFingerprintSafe(oldKey)
                        || !RecipeKey.isWireSafe(oldRecipe)) {
                    continue;
                }
                updated.appendTag(createEntry(keyTag, oldKey, oldRecipe));
            }
        }

        NBTTagCompound root = getRoot(entityData, true);
        if (root != null) {
            root.setTag(listTag, updated);
        }
    }

    private static void forgetEntry(
            @Nullable NBTTagCompound entityData,
            String listTag,
            String keyTag,
            @Nullable String key) {
        if (entityData == null || !isFingerprintSafe(key)) {
            return;
        }

        NBTTagList previous = getEntries(entityData, listTag, false);
        if (previous == null) {
            return;
        }

        NBTTagList updated = new NBTTagList();
        int count = Math.min(previous.tagCount(), MAX_ENTRIES);
        for (int index = 0; index < count; index++) {
            NBTTagCompound entry = previous.getCompoundTagAt(index);
            String oldKey = entry.getString(keyTag);
            String oldRecipe = entry.getString(RECIPE_TAG);
            if (key.equals(oldKey)
                    || !isFingerprintSafe(oldKey)
                    || !RecipeKey.isWireSafe(oldRecipe)) {
                continue;
            }
            updated.appendTag(createEntry(keyTag, oldKey, oldRecipe));
        }

        NBTTagCompound root = getRoot(entityData, true);
        if (root != null) {
            root.setTag(listTag, updated);
        }
    }

    private static int entryCount(@Nullable NBTTagCompound entityData, String listTag) {
        if (entityData == null) {
            return 0;
        }
        NBTTagList entries = getEntries(entityData, listTag, false);
        return entries == null ? 0 : Math.min(entries.tagCount(), MAX_ENTRIES);
    }

    private static NBTTagCompound createEntry(String keyTag, String key, String recipeKey) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString(keyTag, key);
        entry.setString(RECIPE_TAG, recipeKey);
        return entry;
    }

    @Nullable
    private static NBTTagList getEntries(NBTTagCompound entityData, String listTag, boolean create) {
        NBTTagCompound root = getRoot(entityData, create);
        if (root == null) {
            return null;
        }
        if (!root.hasKey(listTag, Constants.NBT.TAG_LIST)) {
            if (!create) {
                return null;
            }
            NBTTagList list = new NBTTagList();
            root.setTag(listTag, list);
            return list;
        }
        return root.getTagList(listTag, Constants.NBT.TAG_COMPOUND);
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
