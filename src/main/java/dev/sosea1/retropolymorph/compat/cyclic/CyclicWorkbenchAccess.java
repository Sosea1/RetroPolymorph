package dev.sosea1.retropolymorph.compat.cyclic;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;

/** Runtime contract injected into Cyclic's persistent workbench container. */
public interface CyclicWorkbenchAccess {

    InventoryCrafting retropolymorph$getCyclicCraftMatrix();

    IInventory retropolymorph$getCyclicCraftResult();
}
