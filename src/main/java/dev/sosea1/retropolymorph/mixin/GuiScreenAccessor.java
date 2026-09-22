package dev.sosea1.retropolymorph.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Client-only access to GuiScreen's live button list.
 *
 * Some legacy addon GUIs (notably AE2 wireless terminals) rebuild their own
 * controls by clearing buttonList and calling initGui() directly after Forge's
 * InitGuiEvent has already fired. RetroPolymorph keeps one stable selector
 * instance and re-attaches it when such a GUI rebuild drops the button.
 */
@Mixin(GuiScreen.class)
public interface GuiScreenAccessor {

    @Accessor("buttonList")
    List<GuiButton> retropolymorph$getButtonList();
}
