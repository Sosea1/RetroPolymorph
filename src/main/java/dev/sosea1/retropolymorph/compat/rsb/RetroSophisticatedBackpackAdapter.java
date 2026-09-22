package dev.sosea1.retropolymorph.compat.rsb;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/** Focused adapter for the Crafting Upgrade in Retro Sophisticated Backpacks. */
public final class RetroSophisticatedBackpackAdapter implements RecipeSelectionAdapter {

    public static final RetroSophisticatedBackpackAdapter INSTANCE =
            new RetroSophisticatedBackpackAdapter();

    private RetroSophisticatedBackpackAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!RsbContainerAccess.recognizes(container)) {
            return AdapterDetectionResult.miss();
        }
        RsbCraftingAccess.ActiveCrafting active =
                RsbCraftingAccess.resolveActiveCrafting(container);
        if (active != null) {
            return AdapterDetectionResult.match(new RetroSophisticatedBackpackContext(container, active));
        }
        return AdapterDetectionResult.blockFallback();
    }
}
