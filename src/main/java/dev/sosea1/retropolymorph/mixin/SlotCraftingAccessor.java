package dev.sosea1.retropolymorph.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Cheap and exact pairing between a vanilla-style crafting result slot and the
 * matrix it consumes. Subclasses of SlotCrafting inherit the transformed field
 * access path, so this also helps conventional modded crafting tables without
 * reflection or class-name checks.
 */
@Mixin(SlotCrafting.class)
public interface SlotCraftingAccessor {

    @Accessor("craftMatrix")
    InventoryCrafting retropolymorph$getCraftMatrix();

    @Accessor("player")
    EntityPlayer retropolymorph$getPlayer();
}
