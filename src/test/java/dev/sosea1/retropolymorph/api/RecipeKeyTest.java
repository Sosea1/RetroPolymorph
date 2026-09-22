package dev.sosea1.retropolymorph.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeKeyTest {

    @Test
    void acceptsNormalForgeAndOpaqueKeys() {
        assertTrue(RecipeKey.isWireSafe("minecraft:stick"));
        assertTrue(RecipeKey.isWireSafe("smelt:minecraft:iron_ore@0>minecraft:iron_ingot@0x1#0"));
    }

    @Test
    void rejectsNullEmptyAndControlCharacters() {
        assertFalse(RecipeKey.isWireSafe(null));
        assertFalse(RecipeKey.isWireSafe(""));
        assertFalse(RecipeKey.isWireSafe("minecraft:stick\nextra"));
        assertFalse(RecipeKey.isWireSafe("minecraft:\u0000stick"));
    }

    @Test
    void rejectsBrokenSurrogates() {
        assertFalse(RecipeKey.isWireSafe("broken-\uD83D"));
        assertFalse(RecipeKey.isWireSafe("broken-\uDE00"));
        assertTrue(RecipeKey.isWireSafe("valid-\uD83D\uDE00"));
    }

    @Test
    void enforcesUtf8ByteLimit() {
        StringBuilder accepted = new StringBuilder();
        for (int i = 0; i < RecipeKey.MAX_UTF8_BYTES; i++) {
            accepted.append('a');
        }
        assertTrue(RecipeKey.isWireSafe(accepted.toString()));
        accepted.append('b');
        assertFalse(RecipeKey.isWireSafe(accepted.toString()));
    }
}
