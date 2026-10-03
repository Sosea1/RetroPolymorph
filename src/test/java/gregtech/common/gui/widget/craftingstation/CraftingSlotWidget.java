package gregtech.common.gui.widget.craftingstation;

import net.minecraft.inventory.Slot;

/** Released GTCEu keeps the server logic null in the client widget. */
public class CraftingSlotWidget {
    private final Object recipeResolver;
    private final Slot slotReference;

    public CraftingSlotWidget(Object recipeResolver, Slot slotReference) {
        this.recipeResolver = recipeResolver;
        this.slotReference = slotReference;
    }
}
