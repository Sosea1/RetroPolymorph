package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Stable identity for one logical recipe conflict, independent of display order. */
public final class ConflictFingerprint {

    private static final String PREFIX = "v1:";

    private ConflictFingerprint() {
    }

    @Nullable
    public static String create(List<RecipeOption> options) {
        if (options == null || options.size() <= 1) {
            return null;
        }

        ArrayList<String> keys = new ArrayList<String>(options.size());
        for (RecipeOption option : options) {
            if (option != null && RecipeKey.isWireSafe(option.getRecipeKey())) {
                keys.add(option.getRecipeKey());
            }
        }
        return createFromKeys(keys);
    }

    @Nullable
    static String createFromKeys(List<String> recipeKeys) {
        if (recipeKeys == null || recipeKeys.size() <= 1) {
            return null;
        }

        Set<String> keys = new TreeSet<String>();
        for (String key : recipeKeys) {
            if (RecipeKey.isWireSafe(key)) {
                keys.add(key);
            }
        }
        if (keys.size() <= 1) {
            return null;
        }

        MessageDigest digest = sha256();
        for (String key : keys) {
            byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
            digest.update((byte) (bytes.length >>> 8));
            digest.update((byte) bytes.length);
            digest.update(bytes);
        }
        return PREFIX + toHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            chars[i * 2] = alphabet[value >>> 4];
            chars[i * 2 + 1] = alphabet[value & 0x0F];
        }
        return new String(chars);
    }
}
