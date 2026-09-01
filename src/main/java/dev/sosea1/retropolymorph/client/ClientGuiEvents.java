package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.Tags;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import dev.sosea1.retropolymorph.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

/**
 * Forge-event integration keeps GUI compatibility logic out of core Mixins.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Tags.MOD_ID)
public final class ClientGuiEvents {

    private static RecipeSelectorController controller;

    private ClientGuiEvents() {
    }

    public static RecipeSelectorController getActiveController() {
        return controller;
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        controller = null;
        ClientSelectionTracker.reset();
    }

    @SubscribeEvent
    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen screen = event.getGui();
        if (!(screen instanceof GuiContainer)) {
            controller = null;
            return;
        }

        if (!PolymorphConfig.isSelectorEnabled()) {
            controller = null;
            JeiTransferIntent.clear();
            return;
        }

        GuiContainer gui = (GuiContainer) screen;
        JeiTransferIntent.discardIfDifferent(gui.inventorySlots);
        SelectionContext context = SelectionContextDetector.detect(gui.inventorySlots);
        if (context == null || context.getResultSlot() == null) {
            controller = null;
            JeiTransferIntent.clear();
            return;
        }

        int windowId = context.getContainer().windowId;
        int sessionToken = ClientSelectionTracker.begin(windowId);

        RecipeSelectorController created = new RecipeSelectorController(
                gui,
                context,
                sessionToken);
        event.getButtonList().add(created.getButton());
        controller = created;

        NetworkHandler.query(windowId, sessionToken);
        created.update();
    }

    @SubscribeEvent
    public static void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        RecipeSelectorController current = controller;
        if (current == null || !(event.getGui() instanceof GuiContainer)) {
            return;
        }

        if (!current.owns((GuiContainer) event.getGui())) {
            return;
        }

        GuiButton clicked = event.getButton();
        if (clicked == current.getButton()) {
            current.toggle();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        RecipeSelectorController current = controller;
        if (current == null || !(event.getGui() instanceof GuiContainer)) {
            return;
        }

        if (!current.owns((GuiContainer) event.getGui())) {
            return;
        }

        current.update();
    }

    @SubscribeEvent
    public static void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        RecipeSelectorController current = controller;
        if (current == null || !(event.getGui() instanceof GuiContainer)) {
            return;
        }

        if (!current.owns((GuiContainer) event.getGui())) {
            return;
        }

        current.drawOverlay(event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        RecipeSelectorController current = controller;
        if (current == null || !(event.getGui() instanceof GuiContainer)) {
            return;
        }

        GuiContainer gui = (GuiContainer) event.getGui();
        if (!current.owns(gui)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        int button = Mouse.getEventButton();
        boolean pressed = button >= 0 && Mouse.getEventButtonState();
        int wheel = Mouse.getEventDWheel();

        if (current.handleMouseInput(mouseX, mouseY, button, pressed, wheel)) {
            event.setCanceled(true);
            return;
        }

        // Right-click button to reset selection
        if (pressed && button == 1 && current.isRightClickClearEnabled()
                && current.getButton().visible
                && contains(current.getButton(), mouseX, mouseY)) {
            current.clearSelection();
            event.setCanceled(true);
        }
    }

    private static boolean contains(GuiButton button, int mouseX, int mouseY) {
        return mouseX >= button.x
                && mouseX < button.x + button.width
                && mouseY >= button.y
                && mouseY < button.y + button.height;
    }
}
