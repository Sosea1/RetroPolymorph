package dev.sosea1.retropolymorph.compat.mekanism;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Focused integration for Mekanism 1.12's manual Formulaic Assemblicator grid. */
public final class MekanismFormulaicAssemblicatorAdapter implements RecipeSelectionAdapter {

    public static final MekanismFormulaicAssemblicatorAdapter INSTANCE =
            new MekanismFormulaicAssemblicatorAdapter();

    private MekanismFormulaicAssemblicatorAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!(container instanceof MekanismFormulaicAssemblicatorAccess)) {
            return AdapterDetectionResult.miss();
        }

        TileEntity tile = ((MekanismFormulaicAssemblicatorAccess) container)
                .retropolymorph$getFormulaicAssemblicatorTile();
        if (!(tile instanceof MekanismFormulaicTileAccess)) {
            return AdapterDetectionResult.blockFallback();
        }

        MekanismFormulaicTileAccess access = (MekanismFormulaicTileAccess) tile;
        List<Slot> inputs = findCraftingGrid(container);
        Slot formulaSlot = findSlot(container, 6, 26);
        Slot anchor = findOutputAnchor(container);
        if (inputs.size() != 9 || formulaSlot == null || anchor == null) {
            return AdapterDetectionResult.blockFallback();
        }

        return AdapterDetectionResult.match(new MekanismFormulaicAssemblicatorContext(
                container,
                tile,
                access,
                inputs,
                formulaSlot,
                anchor));
    }

    private static List<Slot> findCraftingGrid(Container container) {
        List<Slot> result = new ArrayList<>(9);
        for (int y = 17; y <= 53; y += 18) {
            for (int x = 26; x <= 62; x += 18) {
                Slot found = findSlot(container, x, y);
                if (found == null) {
                    return new ArrayList<>();
                }
                result.add(found);
            }
        }
        return result;
    }

    @Nullable
    private static Slot findOutputAnchor(Container container) {
        return findSlot(container, 116, 17);
    }

    @Nullable
    private static Slot findSlot(Container container, int x, int y) {
        for (Slot slot : container.inventorySlots) {
            if (slot.xPos == x && slot.yPos == y) {
                return slot;
            }
        }
        return null;
    }
}
