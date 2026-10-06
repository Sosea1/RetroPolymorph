package com.sosea1.retropolymorph.core;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Weak runtime selection state for optional integrations. */
public final class TransientSelectionStore {

    private static final Object DEFAULT_SCOPE = new Object();

    private final Map<Object, Map<Object, String>> selections =
            new WeakHashMap<Object, Map<Object, String>>();

    @Nullable
    public String get(Object owner) {
        return get(owner, DEFAULT_SCOPE);
    }

    @Nullable
    public synchronized String get(Object owner, Object scope) {
        if (owner == null || scope == null) {
            return null;
        }
        Map<Object, String> scoped = this.selections.get(owner);
        return scoped == null ? null : scoped.get(scope);
    }

    public void set(Object owner, @Nullable String recipeKey) {
        set(owner, DEFAULT_SCOPE, recipeKey);
    }

    public synchronized void set(Object owner, Object scope, @Nullable String recipeKey) {
        if (owner == null || scope == null) {
            return;
        }
        if (recipeKey == null) {
            clear(owner, scope);
            return;
        }
        Map<Object, String> scoped = this.selections.get(owner);
        if (scoped == null) {
            scoped = new HashMap<Object, String>();
            this.selections.put(owner, scoped);
        }
        scoped.put(scope, recipeKey);
    }

    public void clear(Object owner) {
        clear(owner, DEFAULT_SCOPE);
    }

    public synchronized void clear(Object owner, Object scope) {
        if (owner == null || scope == null) {
            return;
        }
        Map<Object, String> scoped = this.selections.get(owner);
        if (scoped == null) {
            return;
        }
        scoped.remove(scope);
        if (scoped.isEmpty()) {
            this.selections.remove(owner);
        }
    }
}
