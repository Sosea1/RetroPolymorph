package gregtech.common.gui.widget;

/** Original GTCE widget, distinct from GTCEu's craftingstation package. */
public class CraftingSlotWidget {
    public final Object recipeResolver;
    public final net.minecraft.inventory.Slot slotReference;
    public CraftingSlotWidget(Object resolver) { this(resolver, null); }
    public CraftingSlotWidget(Object resolver, net.minecraft.inventory.Slot slot) {
        recipeResolver = resolver;
        slotReference = slot;
    }
}
