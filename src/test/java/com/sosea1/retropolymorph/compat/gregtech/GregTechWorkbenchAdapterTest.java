package com.sosea1.retropolymorph.compat.gregtech;

import com.cleanroommc.modularui.screen.ModularContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GregTechWorkbenchAdapterTest {
    @Test
    void unsupportedWorkbenchBindingBlocksGenericCrafting() {
        assertTrue(GregTechWorkbenchAdapter.INSTANCE.probe(new ModularContainer()).isBlockFallback());
    }
}
