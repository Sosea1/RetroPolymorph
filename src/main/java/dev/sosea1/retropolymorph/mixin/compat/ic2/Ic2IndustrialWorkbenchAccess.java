package dev.sosea1.retropolymorph.mixin.compat.ic2;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed access to IC2's otherwise non-vanilla result-slot topology. */
@Pseudo
@Mixin(targets = "ic2.core.block.machine.container.ContainerIndustrialWorkbench", remap = false)
public interface Ic2IndustrialWorkbenchAccess {

    @Accessor(value = "craftMatrix", remap = false)
    InventoryCrafting retropolymorph$getCraftMatrix();

    @Accessor(value = "craftResult", remap = false)
    IInventory retropolymorph$getCraftResult();
}
