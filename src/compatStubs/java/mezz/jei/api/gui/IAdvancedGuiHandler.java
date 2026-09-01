package mezz.jei.api.gui;

import java.awt.Rectangle;
import java.util.List;

public interface IAdvancedGuiHandler<T> {

    Class<T> getGuiContainerClass();

    List<Rectangle> getGuiExtraAreas(T guiContainer);

    Object getIngredientUnderMouse(T guiContainer, int mouseX, int mouseY);
}
