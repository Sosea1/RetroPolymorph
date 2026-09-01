package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2PatternCraftingSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Marker-only optional mixin for AE2 UEL SlotFakeCraftingMatrix. */
@Pseudo
@Mixin(targets = "appeng.container.slot.SlotFakeCraftingMatrix", remap = false)
public abstract class Ae2PatternCraftingSlotMixin implements Ae2PatternCraftingSlot {
}
