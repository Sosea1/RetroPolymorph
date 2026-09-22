package dev.sosea1.retropolymorph.compat.thaumcraft;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/** Focused adapter for Thaumcraft 6's Arcane Workbench recipe engine. */
public final class ThaumcraftArcaneWorkbenchAdapter implements RecipeSelectionAdapter {

    public static final ThaumcraftArcaneWorkbenchAdapter INSTANCE =
            new ThaumcraftArcaneWorkbenchAdapter();

    private static final String RESULT_SLOT =
            "thaumcraft.common.container.slot.SlotCraftingArcaneWorkbench";

    private ThaumcraftArcaneWorkbenchAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!ThaumcraftArcaneReflection.isArcaneWorkbench(container)) {
            return AdapterDetectionResult.miss();
        }
        InventoryCrafting matrix = ThaumcraftArcaneReflection.getMatrix(container);
        EntityPlayer player = ThaumcraftArcaneReflection.getPlayer(container);
        Slot result = findResultSlot(container);
        if (matrix == null || player == null || result == null) {
            return AdapterDetectionResult.blockFallback();
        }
        return AdapterDetectionResult.match(new ThaumcraftArcaneWorkbenchContext(container, matrix, player, result));
    }


    @Nullable
    private static Slot findResultSlot(Container container) {
        for (Slot slot : container.inventorySlots) {
            if (hasClassInHierarchy(slot.getClass(), RESULT_SLOT)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean hasClassInHierarchy(Class<?> type, String targetName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (targetName.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }
}
