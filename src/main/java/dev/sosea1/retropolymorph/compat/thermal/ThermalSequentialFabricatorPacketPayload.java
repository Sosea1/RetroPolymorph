package dev.sosea1.retropolymorph.compat.thermal;

import dev.sosea1.retropolymorph.api.RecipeKey;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Versioned framing for the optional RetroPolymorph field appended to
 * Thermal's native Sequential Fabricator mode packet.
 *
 * <p>The frame is intentionally one string so client and server keep the
 * recipe identity in the same Thermal transaction without introducing a
 * second packet or depending on CoFH internals beyond PacketBase strings.</p>
 */
public final class ThermalSequentialFabricatorPacketPayload {

    private static final String PREFIX = "retropolymorph:thermal_selection:v1:";

    private ThermalSequentialFabricatorPacketPayload() {
    }

    public static String encode(@Nullable ResourceLocation recipeId) {
        return PREFIX + (recipeId == null ? "" : recipeId.toString());
    }

    /**
     * Returns an unrecognized result for foreign trailing strings and malformed
     * frames. An explicit empty v1 frame is the only representation of clear.
     */
    public static Decoded decode(@Nullable String encoded) {
        if (encoded == null || !encoded.startsWith(PREFIX)) {
            return Decoded.unrecognized();
        }
        String recipeKey = encoded.substring(PREFIX.length());
        if (recipeKey.isEmpty()) {
            return Decoded.recognized(null);
        }
        if (!RecipeKey.isWireSafe(recipeKey)) {
            return Decoded.unrecognized();
        }
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        return recipeId == null
                ? Decoded.unrecognized()
                : Decoded.recognized(recipeId);
    }

    /**
     * Non-destructively probes the next modified-UTF field. Foreign or malformed
     * trailing data is rewound so another packet extension can still consume it.
     */
    public static Decoded readOptional(@Nullable DataInputStream input) {
        if (input == null || !input.markSupported()) {
            return Decoded.unrecognized();
        }
        boolean marked = false;
        try {
            int remaining = input.available();
            if (remaining <= 0) {
                return Decoded.unrecognized();
            }
            input.mark(remaining);
            marked = true;
            Decoded decoded = decode(input.readUTF());
            if (!decoded.isRecognized()) {
                input.reset();
            }
            return decoded;
        } catch (IOException ignored) {
            if (marked) {
                try {
                    input.reset();
                } catch (IOException resetIgnored) {
                    // Exact CoFH 1.12 PacketBase uses ByteArrayInputStream, which
                    // supports reset. If a foreign implementation does not, fail
                    // closed and do not stage a recipe selection.
                }
            }
            return Decoded.unrecognized();
        }
    }

    public static final class Decoded {

        private static final Decoded UNRECOGNIZED = new Decoded(false, null);

        private final boolean recognized;
        @Nullable
        private final ResourceLocation recipeId;

        private Decoded(boolean recognized, @Nullable ResourceLocation recipeId) {
            this.recognized = recognized;
            this.recipeId = recipeId;
        }

        private static Decoded unrecognized() {
            return UNRECOGNIZED;
        }

        private static Decoded recognized(@Nullable ResourceLocation recipeId) {
            return new Decoded(true, recipeId);
        }

        public boolean isRecognized() {
            return this.recognized;
        }

        @Nullable
        public ResourceLocation getRecipeId() {
            return this.recipeId;
        }
    }
}
