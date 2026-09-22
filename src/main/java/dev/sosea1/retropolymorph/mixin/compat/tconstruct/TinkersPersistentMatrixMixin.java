package dev.sosea1.retropolymorph.mixin.compat.tconstruct;

import dev.sosea1.retropolymorph.compat.tconstruct.TinkersPersistentMatrixAccess;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** Exposes the shared parent inventory without a hard Tinkers dependency. */
@Pseudo
@Mixin(targets = "slimeknights.tconstruct.shared.inventory.InventoryCraftingPersistent", remap = false)
public abstract class TinkersPersistentMatrixMixin implements TinkersPersistentMatrixAccess {

    @Shadow(remap = false)
    @Final
    private IInventory parent;

    @Override
    public IInventory retropolymorph$getPersistentParent() {
        return this.parent;
    }
}
