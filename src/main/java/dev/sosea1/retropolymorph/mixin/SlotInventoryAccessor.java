package dev.sosea1.retropolymorph.mixin;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exact access to the inventory owned by a Slot. */
@Mixin(Slot.class)
public interface SlotInventoryAccessor {

    @Accessor("inventory")
    IInventory retropolymorph$getInventory();
}
