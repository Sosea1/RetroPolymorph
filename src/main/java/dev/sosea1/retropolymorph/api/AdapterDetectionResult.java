package dev.sosea1.retropolymorph.api;

import javax.annotation.Nullable;

/**
 * Atomic detection outcome from probing a container against an adapter.
 */
public final class AdapterDetectionResult {

    public enum Kind {
        MISS,
        BLOCK_FALLBACK,
        MATCH
    }

    private static final AdapterDetectionResult MISS =
            new AdapterDetectionResult(Kind.MISS, null);
    private static final AdapterDetectionResult BLOCK_FALLBACK =
            new AdapterDetectionResult(Kind.BLOCK_FALLBACK, null);

    private final Kind kind;
    private final SelectionContext context;

    private AdapterDetectionResult(Kind kind, @Nullable SelectionContext context) {
        this.kind = kind;
        this.context = context;
    }

    public static AdapterDetectionResult miss() {
        return MISS;
    }

    public static AdapterDetectionResult blockFallback() {
        return BLOCK_FALLBACK;
    }

    public static AdapterDetectionResult match(SelectionContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        return new AdapterDetectionResult(Kind.MATCH, context);
    }

    public Kind getKind() {
        return this.kind;
    }

    public boolean isMatch() {
        return this.kind == Kind.MATCH;
    }

    public boolean isBlockFallback() {
        return this.kind == Kind.BLOCK_FALLBACK;
    }


    public boolean isMiss() {
        return this.kind == Kind.MISS;
    }

    @Nullable
    public SelectionContext getContext() {
        return this.context;
    }

    @Override
    public String toString() {
        return "AdapterDetectionResult{" +
                "kind=" + this.kind +
                (this.context != null ? ", context=" + this.context.getClass().getSimpleName() : "") +
                '}';
    }
}
