package dev.sosea1.retropolymorph.api;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/** Shared validation policy for opaque recipe keys crossing the network. */
public final class RecipeKey {

    public static final int MAX_UTF8_BYTES = 256;

    private RecipeKey() {
    }

    public static boolean isWireSafe(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        for (int index = 0; index < key.length(); index++) {
            char current = key.charAt(index);
            if (Character.isISOControl(current)) {
                return false;
            }
            if (Character.isHighSurrogate(current)) {
                if (++index >= key.length() || !Character.isLowSurrogate(key.charAt(index))) {
                    return false;
                }
            } else if (Character.isLowSurrogate(current)) {
                return false;
            }
        }
        return key.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
    }

    /** Minecraft 1.12 ResourceLocation parsing does not perform modern regex validation. */
    @Nullable
    public static ResourceLocation parseForgeId(String key) {
        return key == null || key.isEmpty() ? null : new ResourceLocation(key);
    }
}
