package mezz.jei.api;

import mezz.jei.api.gui.IAdvancedGuiHandler;

public interface IModRegistry {

    void addAdvancedGuiHandlers(IAdvancedGuiHandler<?>... advancedGuiHandlers);
}
