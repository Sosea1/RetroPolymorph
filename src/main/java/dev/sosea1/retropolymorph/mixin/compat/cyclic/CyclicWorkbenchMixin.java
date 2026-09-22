package dev.sosea1.retropolymorph.mixin.compat.cyclic;

import com.lothrazar.cyclicmagic.block.workbench.InventoryCraftResultSync;
import com.lothrazar.cyclicmagic.block.workbench.InventoryWorkbench;
import dev.sosea1.retropolymorph.compat.cyclic.CyclicWorkbenchAccess;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** Typed bridge for Cyclic fields whose declared types are mod-specific subclasses. */
@Pseudo
@Mixin(targets = "com.lothrazar.cyclicmagic.block.workbench.ContainerWorkBench", remap = false)
public abstract class CyclicWorkbenchMixin implements CyclicWorkbenchAccess {

    @Shadow(remap = false)
    private InventoryWorkbench craftMatrix;

    @Shadow(remap = false)
    private InventoryCraftResultSync craftResult;

    @Override
    public InventoryCrafting retropolymorph$getCyclicCraftMatrix() {
        return (InventoryCrafting) (Object) this.craftMatrix;
    }

    @Override
    public IInventory retropolymorph$getCyclicCraftResult() {
        return (IInventory) (Object) this.craftResult;
    }
}
