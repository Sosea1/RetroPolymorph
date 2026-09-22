package dev.sosea1.retropolymorph.config;

/**
 * Client-side selector presentation mode.
 *
 * COMPACT is RetroPolymorph's default UX: a five-choice viewport with
 * arrow/wheel navigation. CLASSIC mirrors upstream Polymorph's centered
 * horizontal list when it fits, and falls back to a viewport on narrow screens.
 */
public enum SelectorMode {
    COMPACT,
    CLASSIC;

    public static SelectorMode parse(String value) {
        if (value == null) {
            return COMPACT;
        }
        String normalized = value.trim();
        if ("CLASSIC".equalsIgnoreCase(normalized) || "EXPANDED".equalsIgnoreCase(normalized)) {
            return CLASSIC;
        }
        if ("COMPACT".equalsIgnoreCase(normalized) || "PAGED".equalsIgnoreCase(normalized)) {
            return COMPACT;
        }
        return COMPACT;
    }
}
