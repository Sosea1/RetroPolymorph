package gregtech.api.gui.impl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/** Signature fixture for GTCEu's released, pre-ModularUI2 container. */
public class ModularUIContainer extends Container {
    private final Object modularUI;

    public ModularUIContainer(Object modularUI) {
        this.modularUI = modularUI;
    }

    public Object getModularUI() {
        return this.modularUI;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
