package dev.sosea1.retropolymorph.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Client-only access to the vanilla GUI origin for positioning the selector
 * next to the actual crafting result slot.
 */
@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {

    @Accessor("guiLeft")
    int retropolymorph$getGuiLeft();

    @Accessor("guiTop")
    int retropolymorph$getGuiTop();
}
