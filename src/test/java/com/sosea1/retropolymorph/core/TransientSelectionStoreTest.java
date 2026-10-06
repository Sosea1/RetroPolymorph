package com.sosea1.retropolymorph.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TransientSelectionStoreTest {

    @Test
    public void defaultScopeCanSetReplaceAndClear() {
        TransientSelectionStore store = new TransientSelectionStore();
        Object owner = new Object();

        assertNull(store.get(owner));
        store.set(owner, "example:first");
        assertEquals("example:first", store.get(owner));
        store.set(owner, "example:second");
        assertEquals("example:second", store.get(owner));
        store.clear(owner);
        assertNull(store.get(owner));
    }

    @Test
    public void scopesAreIndependent() {
        TransientSelectionStore store = new TransientSelectionStore();
        Object owner = new Object();
        Object first = new Object();
        Object second = new Object();

        store.set(owner, first, "example:first");
        store.set(owner, second, "example:second");
        assertEquals("example:first", store.get(owner, first));
        assertEquals("example:second", store.get(owner, second));

        store.clear(owner, first);
        assertNull(store.get(owner, first));
        assertEquals("example:second", store.get(owner, second));
    }

    @Test
    public void nullValueClearsOnlyRequestedScope() {
        TransientSelectionStore store = new TransientSelectionStore();
        Object owner = new Object();
        Object scope = new Object();
        store.set(owner, scope, "example:first");
        store.set(owner, scope, null);
        assertNull(store.get(owner, scope));
    }
}
