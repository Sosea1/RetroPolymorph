package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.compat.tconstruct.TinkersSharedSelectionRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifecycleResetCharacterizationTest {

    private static final class TestContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    }

    @BeforeAll
    static void setup() {
        Bootstrap.register();
    }

    @Test
    @DisplayName("SharedSelectionViewerRegistry resets all tracked owners and viewers")
    void testSharedSelectionViewerRegistryReset() {
        Object owner = new Object();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        SharedSelectionViewerRegistry.register(owner, player1, 1);
        SharedSelectionViewerRegistry.register(owner, player2, 2);

        assertEquals(1, SharedSelectionViewerRegistry.getTrackedOwnerCount());
        assertEquals(2, SharedSelectionViewerRegistry.getViewers(owner).size());

        // Trigger lifecycle reset
        SharedSelectionViewerRegistry.reset();

        assertEquals(0, SharedSelectionViewerRegistry.getTrackedOwnerCount());
        assertEquals(0, SharedSelectionViewerRegistry.getViewers(owner).size());
    }

    @Test
    @DisplayName("TinkersSharedSelectionRegistry resets all bound stations")
    void testTinkersSharedSelectionRegistryReset() {
        slimeknights.tconstruct.tools.common.inventory.ContainerCraftingStation container =
                new slimeknights.tconstruct.tools.common.inventory.ContainerCraftingStation();

        TinkersSharedSelectionRegistry.bind(container, container.getMatrix());
        assertEquals(1, TinkersSharedSelectionRegistry.getBoundStationCount());

        // Trigger lifecycle reset
        TinkersSharedSelectionRegistry.reset();

        assertEquals(0, TinkersSharedSelectionRegistry.getBoundStationCount());
    }
}
