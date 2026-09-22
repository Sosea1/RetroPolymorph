package dev.sosea1.retropolymorph.preference;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class ConflictFingerprintTest {

    @Test
    public void orderDoesNotChangeFingerprint() {
        String first = ConflictFingerprint.createFromKeys(Arrays.asList(
                "moda:first", "modb:second", "modc:third"));
        String second = ConflictFingerprint.createFromKeys(Arrays.asList(
                "modc:third", "moda:first", "modb:second"));
        assertEquals(first, second);
    }

    @Test
    public void recipeSetChangeChangesFingerprint() {
        String first = ConflictFingerprint.createFromKeys(Arrays.asList(
                "moda:first", "modb:second"));
        String second = ConflictFingerprint.createFromKeys(Arrays.asList(
                "moda:first", "modc:third"));
        assertNotEquals(first, second);
    }

    @Test
    public void duplicateKeysDoNotChangeFingerprint() {
        String first = ConflictFingerprint.createFromKeys(Arrays.asList(
                "moda:first", "modb:second"));
        String second = ConflictFingerprint.createFromKeys(Arrays.asList(
                "moda:first", "modb:second", "moda:first"));
        assertEquals(first, second);
    }

    @Test
    public void singleRecipeIsNotAConflict() {
        assertNull(ConflictFingerprint.createFromKeys(Arrays.asList("moda:first")));
    }
}
