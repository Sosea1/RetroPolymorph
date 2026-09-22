package dev.sosea1.retropolymorph.mixin.compat.mekanism;

import dev.sosea1.retropolymorph.compat.mekanism.MekanismFormulaicAssemblicatorAccess;
import mekanism.common.inventory.container.ContainerFormulaicAssemblicator;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** Captures the Formulaic Assemblicator tile without depending on private container fields. */
@Pseudo
@Mixin(value = ContainerFormulaicAssemblicator.class, remap = false)
public abstract class MekanismFormulaicContainerMixin implements MekanismFormulaicAssemblicatorAccess {

    @Unique
    private TileEntity retropolymorph$formulaicTile;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void retropolymorph$captureTile(
            InventoryPlayer inventory,
            TileEntityFormulaicAssemblicator tile,
            CallbackInfo ci) {
        this.retropolymorph$formulaicTile = tile;
    }

    @Override
    @Nullable
    public TileEntity retropolymorph$getFormulaicAssemblicatorTile() {
        return this.retropolymorph$formulaicTile;
    }
}
