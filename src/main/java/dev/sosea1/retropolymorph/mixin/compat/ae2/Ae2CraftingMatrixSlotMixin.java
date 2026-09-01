package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2CraftingMatrixSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Marker-only optional mixin for AE2 UEL SlotCraftingMatrix. */
@Pseudo
@Mixin(targets = "appeng.container.slot.SlotCraftingMatrix", remap = false)
public abstract class Ae2CraftingMatrixSlotMixin implements Ae2CraftingMatrixSlot {
}
