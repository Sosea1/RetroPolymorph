package dev.sosea1.retropolymorph.compat.thermal;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ThermalSequentialFabricatorSelectionStoreTest {

    @Test
    void noOpWritesDoNotDirtyTile() {
        TrackingTile tile = new TrackingTile();
        ResourceLocation recipe = new ResourceLocation("minecraft", "stick");

        ThermalSequentialFabricatorSelectionStore.writeSelectedRecipeId(tile, null);
        assertEquals(0, tile.dirtyCalls);

        ThermalSequentialFabricatorSelectionStore.writeSelectedRecipeId(tile, recipe);
        assertEquals(1, tile.dirtyCalls);
        assertEquals(recipe, ThermalSequentialFabricatorSelectionStore.getSelectedRecipeId(tile));

        ThermalSequentialFabricatorSelectionStore.writeSelectedRecipeId(tile, recipe);
        assertEquals(1, tile.dirtyCalls);

        ThermalSequentialFabricatorSelectionStore.writeSelectedRecipeId(tile, null);
        assertEquals(2, tile.dirtyCalls);
        assertNull(ThermalSequentialFabricatorSelectionStore.getSelectedRecipeId(tile));

        ThermalSequentialFabricatorSelectionStore.writeSelectedRecipeId(tile, null);
        assertEquals(2, tile.dirtyCalls);
    }

    private static final class TrackingTile extends TileEntity {
        private int dirtyCalls;

        @Override
        public void markDirty() {
            this.dirtyCalls++;
        }
    }
}
