package dev.sosea1.retropolymorph.compat.jei;

import dev.sosea1.retropolymorph.client.ClientGuiEvents;
import dev.sosea1.retropolymorph.client.RecipeSelectorController;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import net.minecraft.client.gui.inventory.GuiContainer;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PolymorphAdvancedGuiHandler implements IAdvancedGuiHandler<GuiContainer> {

    @Override
    public Class<GuiContainer> getGuiContainerClass() {
        return GuiContainer.class;
    }

    @Nullable
    @Override
    public List<Rectangle> getGuiExtraAreas(GuiContainer guiContainer) {
        RecipeSelectorController controller = ClientGuiEvents.getActiveController();
        if (controller == null || !controller.owns(guiContainer) || !controller.isVisible()) {
            return Collections.emptyList();
        }

        List<Rectangle> areas = new ArrayList<Rectangle>(2);
        areas.add(new Rectangle(
                controller.getButtonX(),
                controller.getButtonY(),
                controller.getButtonWidth(),
                controller.getButtonHeight()));

        if (controller.isExpanded()) {
            areas.add(new Rectangle(
                    controller.getPanelLeft(),
                    controller.getPanelTop(),
                    controller.getPanelWidth(),
                    controller.getPanelHeight()));
        }

        return areas;
    }

    @Nullable
    @Override
    public Object getIngredientUnderMouse(GuiContainer guiContainer, int mouseX, int mouseY) {
        return null;
    }
}
