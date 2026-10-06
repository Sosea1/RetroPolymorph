package com.cleanroommc.retrosophisticatedbackpacks.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraftforge.items.ItemStackHandler;
import java.util.HashMap;
import java.util.Map;

/** Test fixture for the RSB container's reflective topology contract. */
public final class BackpackContainer extends Container {
    public final Map<Integer, InventoryCrafting> inventoryCraftingInstances = new HashMap<>();
    public final Map<Integer, Slot> craftingSlotInstances = new HashMap<>();
    public final Wrapper wrapper = new Wrapper();
    public int outputRefreshes;

    @Override public boolean canInteractWith(EntityPlayer player) { return false; }
    @Override public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) { outputRefreshes++; }

    public static final class Wrapper {
        public final ItemStackHandler upgrades = new ItemStackHandler(2);
        public ItemStackHandler getUpgradeItemStackHandler() { return upgrades; }
    }
}
