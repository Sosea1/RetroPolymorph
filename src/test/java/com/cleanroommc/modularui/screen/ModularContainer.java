package com.cleanroommc.modularui.screen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * Test stub for ModularContainer.
 */
public class ModularContainer extends Container {

    private Object syncManager;

    public void setSyncManager(Object syncManager) {
        this.syncManager = syncManager;
    }

    public Object getSyncManager() {
        return this.syncManager;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
}
