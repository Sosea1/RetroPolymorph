package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import dev.sosea1.retropolymorph.diagnostic.CraftingDetectionDiagnostics;
import dev.sosea1.retropolymorph.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnaceOutput;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge-event integration keeps GUI compatibility logic out of core Mixins.
 */
public final class ClientGuiEvents {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    private static final Class<?> MANTLE_GUI_MODULE_CLASS;

    static {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("slimeknights.mantle.client.gui.GuiModule");
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
        MANTLE_GUI_MODULE_CLASS = clazz;
    }

    private static RecipeSelectorController controller;

    public ClientGuiEvents() {
    }

    public static RecipeSelectorController getActiveController() {
        return controller;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        controller = null;
        ClientSelectionTracker.reset();
        SelectorQueryScheduler.cancel();
    }

    @SubscribeEvent
    public void onClientDisconnect(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        controller = null;
        ClientSelectionTracker.reset();
        SelectorQueryScheduler.cancel();
    }

    @SubscribeEvent
    public void onClientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
            SelectorQueryScheduler.onTickEnd();
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen screen = event.getGui();
        if (!(screen instanceof GuiContainer)) {
            controller = null;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (screen != mc.currentScreen || (MANTLE_GUI_MODULE_CLASS != null && MANTLE_GUI_MODULE_CLASS.isInstance(screen))) {
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
        if (context == null || !context.getSelectorPlacement().isVisible()) {
            if (LOGGER.isDebugEnabled()) {
                CraftingDetectionDiagnostics.Report report =
                        CraftingDetectionDiagnostics.inspect(gui.inventorySlots);
                LOGGER.debug(
                        "GUI probe unsupported: gui={}, container={}, route={}, slots={}, slotCrafting={}, "
                                + "accessors={}, matrices={}, resultInventories={}, detail={}, furnaceSlots={}",
                        gui.getClass().getName(),
                        report.containerClass,
                        report.route,
                        report.totalSlots,
                        report.slotCraftingCount,
                        report.slotCraftingAccessorCount,
                        report.craftingMatrixCount,
                        report.resultInventoryCount,
                        report.detail,
                        describeFurnaceSlots(gui));
            }
            controller = null;
            JeiTransferIntent.clear();
            return;
        }

        int windowId = context.getContainer().windowId;
        int sessionToken = ClientSelectionTracker.begin(windowId);
        LOGGER.debug(
                "GUI probe supported: gui={}, container={}, context={}, window={}",
                gui.getClass().getName(),
                context.getContainer().getClass().getName(),
                context.getClass().getName(),
                Integer.valueOf(windowId));

        RecipeSelectorController created = new RecipeSelectorController(
                gui,
                context,
                sessionToken);
        event.getButtonList().add(created.getButton());
        controller = created;

        created.update();
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
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
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        RecipeSelectorController current = controller;
        if (current == null || !(event.getGui() instanceof GuiContainer)) {
            return;
        }

        if (!current.owns((GuiContainer) event.getGui())) {
            return;
        }

        current.update();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
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
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer)) {
            return;
        }
        if (!Keyboard.getEventKeyState()) {
            return;
        }

        GuiContainer gui = (GuiContainer) event.getGui();
        int keyCode = Keyboard.getEventKey();
        RecipeSelectorController current = controller;
        if (current != null && current.owns(gui) && current.handleKeyboardInput(keyCode)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
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
        }
    }

    private static String describeFurnaceSlots(GuiContainer gui) {
        StringBuilder details = new StringBuilder();
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (!(slot instanceof SlotFurnaceOutput)) {
                continue;
            }
            if (details.length() > 0) {
                details.append(';');
            }
            details.append("slotClass=").append(slot.getClass().getName())
                    .append(",slotIndex=").append(slot.getSlotIndex())
                    .append(",inventoryClass=").append(slot.inventory.getClass().getName())
                    .append(",inventorySize=").append(slot.inventory.getSizeInventory())
                    .append(",tileEntityFurnace=")
                    .append(slot.inventory instanceof TileEntityFurnace);
        }
        return details.length() == 0 ? "none" : details.toString();
    }
}
