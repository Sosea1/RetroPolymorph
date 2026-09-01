package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Marker-only optional mixin for AE2 UEL SlotCraftingTerm. */
@Pseudo
@Mixin(targets = {
        "appeng.container.slot.SlotCraftingTerm",
        "p455w0rd.wct.container.slot.SlotCraftingOutput"
}, remap = false)
public abstract class Ae2CraftingResultSlotMixin implements Ae2CraftingResultSlot {
}
