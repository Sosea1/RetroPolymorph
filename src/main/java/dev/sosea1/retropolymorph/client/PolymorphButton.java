package dev.sosea1.retropolymorph.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * Dedicated icon button for recipe selection with authentic Polymorph visuals.
 */
public class PolymorphButton extends GuiButton {

    public static final int BUTTON_SIZE = 16;
    private static final ResourceLocation TEXTURE_NORMAL =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/selector_button.png");
    private static final ResourceLocation TEXTURE_HIGHLIGHTED =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/selector_button_highlighted.png");

    private boolean hasSelection;
    private boolean errorState;

    public PolymorphButton(int buttonId, int x, int y) {
        super(buttonId, x, y, BUTTON_SIZE, BUTTON_SIZE, "");
    }

    public void setState(boolean hasSelection, boolean errorState) {
        this.hasSelection = hasSelection;
        this.errorState = errorState;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        ResourceLocation tex = this.hovered ? TEXTURE_HIGHLIGHTED : TEXTURE_NORMAL;
        mc.getTextureManager().bindTexture(tex);
        Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, 0, 0, this.width, this.height, 16, 16);

        // A selected recipe must not look permanently hovered. Keep the normal
        // button sprite and use a tiny top-right status dot instead:
        // green = explicit selection, red = server rejected/invalidated the
        // last action.
        if (this.errorState) {
            drawStatusCorner(0xFFE14B4B);
        } else if (this.hasSelection) {
            drawStatusCorner(0xFF55E39A);
        }

        GlStateManager.disableBlend();
    }

    private void drawStatusCorner(int color) {
        final int outerLeft = this.x + 12;
        final int outerTop = this.y;
        final int outerRight = this.x + 16;
        final int outerBottom = this.y + 4;
        Gui.drawRect(outerLeft, outerTop, outerRight, outerBottom, 0xFF101010);
        Gui.drawRect(outerLeft + 1, outerTop + 1, outerRight - 1, outerBottom - 1, color);
    }
}
