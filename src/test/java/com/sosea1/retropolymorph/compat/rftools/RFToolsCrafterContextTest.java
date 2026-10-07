package com.sosea1.retropolymorph.compat.rftools;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RFToolsCrafterContextTest {
    @BeforeAll
    static void bootstrap() { Bootstrap.register(); }

    @Test
    void recreatedContextKeepsChoiceButChangedTemplateDoesNot() {
        Container container = new Container() {
            public boolean canInteractWith(EntityPlayer player) { return true; }
        };
        InventoryBasic inventory = new InventoryBasic("template", false, 10);
        inventory.setInventorySlotContents(4, new ItemStack(Blocks.DIRT));
        Slot[] inputs = new Slot[9];
        for (int i = 0; i < 9; i++) { inputs[i] = new Slot(inventory, i, 0, 0); }
        Slot output = new Slot(inventory, 9, 0, 0);
        RFToolsCrafterContext first = new RFToolsCrafterContext(container, inputs, output);
        first.applyRemoteSelection("test:chosen");
        RFToolsCrafterContext second = new RFToolsCrafterContext(container, inputs, output);
        assertEquals("test:chosen", second.getSelectedRecipeKey());
        inventory.setInventorySlotContents(0, new ItemStack(Blocks.DIRT));
        assertNull(second.getSelectedRecipeKey());
    }

    @Test
    void machineTemplateDoesNotWritePersonalPreferences() {
        Container container = new Container() {
            public boolean canInteractWith(EntityPlayer player) { return true; }
        };
        InventoryBasic inventory = new InventoryBasic("template", false, 10);
        Slot[] inputs = new Slot[9];
        for (int i = 0; i < 9; i++) { inputs[i] = new Slot(inventory, i, 0, 0); }
        assertFalse(new RFToolsCrafterContext(container, inputs, new Slot(inventory, 9, 0, 0))
                .getPersistencePolicy().supportsPlayerPreferences());
    }
}
