package mekanism.common.inventory.container;

import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

/** Compile-only signature stub; never packaged. */
public class ContainerFormulaicAssemblicator extends Container {

    public ContainerFormulaicAssemblicator(InventoryPlayer inventory, TileEntityFormulaicAssemblicator tile) {
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
}
