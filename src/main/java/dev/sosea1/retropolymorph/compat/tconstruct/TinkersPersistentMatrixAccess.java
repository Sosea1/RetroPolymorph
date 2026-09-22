package dev.sosea1.retropolymorph.compat.tconstruct;

import net.minecraft.inventory.IInventory;

import javax.annotation.Nullable;

/**
 * Optional bridge mixed into Tinkers' InventoryCraftingPersistent.
 *
 * <p>Every open Crafting Station container owns a different crafting-matrix
 * wrapper, but all wrappers delegate their item stacks to the same parent tile
 * inventory. That parent is therefore the stable identity for a shared station
 * selection while one or more viewers have it open.</p>
 */
public interface TinkersPersistentMatrixAccess {

    @Nullable
    IInventory retropolymorph$getPersistentParent();
}
