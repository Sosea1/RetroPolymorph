package dev.sosea1.retropolymorph.mixin.compat.refinedstorage;

import com.raoulvdberge.refinedstorage.api.network.grid.GridType;
import com.raoulvdberge.refinedstorage.api.network.grid.IGrid;
import dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStorageCraftingGridAccess;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

/** Bridges RS's ContainerGrid to a dependency-free RetroPolymorph contract. */
@Pseudo
@Mixin(targets = "com.raoulvdberge.refinedstorage.container.ContainerGrid", remap = false)
public abstract class RefinedStorageContainerGridMixin implements RefinedStorageCraftingGridAccess {

    @Shadow(remap = false)
    private IGrid grid;

    @Override
    public boolean retropolymorph$isRefinedStorageCraftingGrid() {
        return this.grid != null && this.grid.getGridType() == GridType.CRAFTING;
    }

    @Override
    public boolean retropolymorph$isRefinedStoragePatternGrid() {
        return this.grid != null && this.grid.getGridType() == GridType.PATTERN;
    }

    @Override
    public boolean retropolymorph$isRefinedStorageProcessingPattern() {
        return this.grid instanceof dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStoragePatternGridStateAccess
                && ((dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStoragePatternGridStateAccess) this.grid)
                        .retropolymorph$isRefinedStorageProcessingPattern();
    }

    @Override
    @Nullable
    public InventoryCrafting retropolymorph$getRefinedStorageCraftingMatrix() {
        return this.grid == null ? null : this.grid.getCraftingMatrix();
    }

    @Override
    public void retropolymorph$refreshRefinedStorageCraftingMatrix() {
        if (this.grid != null) {
            this.grid.onCraftingMatrixChanged();
        }
    }
}
