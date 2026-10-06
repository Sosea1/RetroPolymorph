package com.cleanroommc.retrosophisticatedbackpacks.common.gui;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

public final class IndexedModularCraftingSlot extends Slot {
    public final int upgradeSlotIndex;
    public IndexedModularCraftingSlot(InventoryCrafting matrix, int index) {
        super(matrix, 9, 0, 0);
        upgradeSlotIndex = index;
    }
}
