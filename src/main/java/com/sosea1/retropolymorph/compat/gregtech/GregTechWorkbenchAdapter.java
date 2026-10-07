package com.sosea1.retropolymorph.compat.gregtech;

import com.sosea1.retropolymorph.api.AdapterDetectionResult;
import com.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import net.minecraft.inventory.Container;

public final class GregTechWorkbenchAdapter implements RecipeSelectionAdapter {

    public static final GregTechWorkbenchAdapter INSTANCE = new GregTechWorkbenchAdapter();

    private GregTechWorkbenchAdapter() {
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (!GregTechWorkbenchReflection.isSupportedContainer(container)) {
            return AdapterDetectionResult.miss();
        }

        GregTechWorkbenchReflection.Binding binding =
                GregTechWorkbenchReflection.bind(container);
        if (binding == null) {
            return AdapterDetectionResult.blockFallback();
        }

        return AdapterDetectionResult.match(
                new GregTechWorkbenchContext(container, binding));
    }
}
