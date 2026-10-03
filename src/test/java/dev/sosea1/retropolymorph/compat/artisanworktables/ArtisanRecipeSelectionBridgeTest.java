package dev.sosea1.retropolymorph.compat.artisanworktables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ArtisanRecipeSelectionBridgeTest {

    @AfterEach
    public void cleanup() {
        ArtisanRecipeSelectionBridge.endProbe();
        ArtisanRecipeSelectionBridge.endSelection();
    }

    @Test
    public void selectedRecipeIsVirtualLastWithoutMutatingList() {
        Object first = new Object();
        Object selected = new Object();
        Object last = new Object();
        List<Object> recipes = new ArrayList<Object>(Arrays.asList(first, selected, last));

        ArtisanRecipeSelectionBridge.beginProbe(selected);

        assertSame(selected, ArtisanRecipeSelectionBridge.get(recipes, 2));
        assertSame(last, ArtisanRecipeSelectionBridge.get(recipes, 1));
        assertSame(first, ArtisanRecipeSelectionBridge.get(recipes, 0));
        assertEquals(Arrays.asList(first, selected, last), recipes);
    }

    @Test
    public void virtualRemoveMapsBackToRealIndex() {
        Object first = new Object();
        Object selected = new Object();
        Object fallback = new Object();
        List<Object> recipes = new ArrayList<Object>(Arrays.asList(first, selected, fallback));

        ArtisanRecipeSelectionBridge.beginProbe(selected);

        assertSame(fallback, ArtisanRecipeSelectionBridge.remove(recipes, 1));
        assertEquals(Arrays.asList(first, selected), recipes);
    }
}
