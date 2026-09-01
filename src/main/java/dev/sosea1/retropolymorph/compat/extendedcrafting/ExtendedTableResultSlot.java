package dev.sosea1.retropolymorph.compat.extendedcrafting;

import net.minecraft.inventory.InventoryCrafting;

/** Marker/bridge mixed into Extended Crafting's TableResultHandler. */
public interface ExtendedTableResultSlot {

    InventoryCrafting retropolymorph$getExtendedCraftingMatrix();
}
