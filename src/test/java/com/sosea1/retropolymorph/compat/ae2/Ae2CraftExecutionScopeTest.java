package com.sosea1.retropolymorph.compat.ae2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Ae2CraftExecutionScopeTest {
    @AfterEach void cleanup() { Ae2CraftExecutionScope.onContainerClosed(null); }

    @Test
    void throwingCraftClosesItsExecutionScope() {
        assertThrows(IllegalStateException.class, () -> {
            try (Ae2CraftExecutionScope.Scope ignored = Ae2CraftExecutionScope.open(null)) {
                assertTrue(Ae2CraftExecutionScope.isActive());
                throw new IllegalStateException("native craft failed");
            }
        });
        assertFalse(Ae2CraftExecutionScope.isActive());
    }

    @Test
    void closingFailedInnerPinTwiceKeepsOuterScope() {
        try (Ae2CraftExecutionScope.Scope outer = Ae2CraftExecutionScope.open(null)) {
            try (Ae2CraftExecutionScope.Scope inner = Ae2CraftExecutionScope.open(null)) {
                inner.close();
                assertTrue(Ae2CraftExecutionScope.isActive());
            }
            assertTrue(Ae2CraftExecutionScope.isActive());
        }
        assertFalse(Ae2CraftExecutionScope.isActive());
    }
}
