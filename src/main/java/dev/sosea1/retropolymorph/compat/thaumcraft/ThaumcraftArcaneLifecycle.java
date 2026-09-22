package dev.sosea1.retropolymorph.compat.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;

/** Keeps Arcane Workbench selection GUI-scoped; player preferences provide persistence. */
public final class ThaumcraftArcaneLifecycle {

    private ThaumcraftArcaneLifecycle() {
    }

    public static void clearContainerSelection(Container container, EntityPlayer player) {
        InventoryCrafting matrix = ThaumcraftArcaneReflection.getMatrix(container);
        if (matrix != null) {
            ThaumcraftArcaneSelectionStore.clear(matrix, player);
        }
    }
}
