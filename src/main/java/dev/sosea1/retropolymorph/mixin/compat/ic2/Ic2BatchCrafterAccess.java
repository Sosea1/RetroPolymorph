package dev.sosea1.retropolymorph.mixin.compat.ic2;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow bridge to IC2's vanilla-recipe-backed Batch Crafter. */
@Pseudo
@Mixin(targets = "ic2.core.block.machine.tileentity.TileEntityBatchCrafter", remap = false)
public interface Ic2BatchCrafterAccess {

    @Accessor(value = "crafting", remap = false)
    InventoryCrafting retropolymorph$getBatchCraftingMatrix();

    @Accessor(value = "recipe", remap = false)
    void retropolymorph$setBatchRecipe(IRecipe recipe);

    @Invoker(value = "matrixChange", remap = false)
    void retropolymorph$recalculateBatchRecipe(int changedSlot);
}
