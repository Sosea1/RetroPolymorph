package dev.sosea1.retropolymorph.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SelectorModeTest {

    @Test
    public void compactIsSafeDefault() {
        assertEquals(SelectorMode.COMPACT, SelectorMode.parse(null));
        assertEquals(SelectorMode.COMPACT, SelectorMode.parse(""));
        assertEquals(SelectorMode.COMPACT, SelectorMode.parse("unknown"));
    }

    @Test
    public void acceptsCanonicalModeNamesCaseInsensitively() {
        assertEquals(SelectorMode.COMPACT, SelectorMode.parse("compact"));
        assertEquals(SelectorMode.CLASSIC, SelectorMode.parse("CLASSIC"));
    }

    @Test
    public void acceptsOldPlanningAliasesWithoutChangingConfigSemantics() {
        assertEquals(SelectorMode.COMPACT, SelectorMode.parse("paged"));
        assertEquals(SelectorMode.CLASSIC, SelectorMode.parse("expanded"));
    }
}
