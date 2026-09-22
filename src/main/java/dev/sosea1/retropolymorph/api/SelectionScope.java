package dev.sosea1.retropolymorph.api;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Metadata indicating whether recipe selection is private to this container context
 * or shared among multiple players/views of the same underlying owner.
 */
public final class SelectionScope {

    public enum Kind {
        LOCAL,
        SHARED
    }

    private static final SelectionScope LOCAL_INSTANCE =
            new SelectionScope(Kind.LOCAL, null);

    private final Kind kind;
    private final Object ownerIdentity;

    private SelectionScope(Kind kind, @Nullable Object ownerIdentity) {
        this.kind = kind;
        this.ownerIdentity = ownerIdentity;
    }

    public static SelectionScope local() {
        return LOCAL_INSTANCE;
    }

    public static SelectionScope shared(Object ownerIdentity) {
        if (ownerIdentity == null) {
            throw new NullPointerException("ownerIdentity");
        }
        return new SelectionScope(Kind.SHARED, ownerIdentity);
    }

    public Kind getKind() {
        return this.kind;
    }

    public boolean isShared() {
        return this.kind == Kind.SHARED;
    }

    @Nullable
    public Object getOwnerIdentity() {
        return this.ownerIdentity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectionScope that = (SelectionScope) o;
        return this.kind == that.kind && this.ownerIdentity == that.ownerIdentity;
    }

    @Override
    public int hashCode() {
        int result = this.kind.hashCode();
        result = 31 * result + (this.ownerIdentity != null ? System.identityHashCode(this.ownerIdentity) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SelectionScope{" +
                "kind=" + this.kind +
                ", ownerIdentity=" + this.ownerIdentity +
                '}';
    }
}
