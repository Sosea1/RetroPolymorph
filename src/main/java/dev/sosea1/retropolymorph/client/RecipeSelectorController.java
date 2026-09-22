package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.mixin.GuiContainerAccessor;
import dev.sosea1.retropolymorph.mixin.GuiScreenAccessor;
import dev.sosea1.retropolymorph.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.util.List;

public final class RecipeSelectorController {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    private static final int BUTTON_ID = 0x504345;

    private final GuiContainer gui;
    private final SelectionContext context;
    private final int sessionToken;
    private final PolymorphButton button;
    private final ClientRecipeCache cache = new ClientRecipeCache();
    private final SelectorLayout layout;
    private final SelectorNavigationState navigation = new SelectorNavigationState();
    private final RecipeSelectorRenderer renderer;

    private final int buttonOffsetX;
    private final int buttonOffsetY;
    private final boolean rightClickClears;
    private final boolean wheelCyclesButton;
    private final boolean closeAfterSelection;

    private boolean expanded;
    private boolean buttonReattachLogged;
    private int inputRevision;
    private long appliedSelectionRevision;

    RecipeSelectorController(
            GuiContainer gui,
            SelectionContext context,
            int sessionToken) {
        this.gui = gui;
        this.context = context;
        this.sessionToken = sessionToken;
        this.button = new PolymorphButton(BUTTON_ID, 0, 0);
        this.button.visible = false;
        this.layout = new SelectorLayout(PolymorphConfig.getSelectorMode());

        this.buttonOffsetX = PolymorphConfig.getButtonOffsetX();
        this.buttonOffsetY = PolymorphConfig.getButtonOffsetY();
        this.renderer = new RecipeSelectorRenderer(
                PolymorphConfig.isShowRecipeKeyInTooltip(),
                PolymorphConfig.isShowRecipeSourceInTooltip());
        this.rightClickClears = PolymorphConfig.isRightClickClears();
        this.wheelCyclesButton = PolymorphConfig.isWheelCyclesButton();
        this.closeAfterSelection = PolymorphConfig.isCloseAfterSelection();

        this.appliedSelectionRevision = ClientSelectionTracker.getRevision(
                context.getContainer().windowId,
                sessionToken);
        updateButtonPosition();
        SelectorPlacement initialPlacement = context.getSelectorPlacement();
        if (initialPlacement.getMode() == SelectorPlacement.AnchorMode.GUI_TOP_RIGHT) {
            GuiContainerAccessor accessor = (GuiContainerAccessor) gui;
            LOGGER.debug(
                    "Selector GUI-corner anchor active: gui={}, container={}, bounds={},{},{}x{}, button={},{}",
                    gui.getClass().getName(),
                    context.getContainer().getClass().getName(),
                    Integer.valueOf(accessor.retropolymorph$getGuiLeft()),
                    Integer.valueOf(accessor.retropolymorph$getGuiTop()),
                    Integer.valueOf(accessor.retropolymorph$getXSize()),
                    Integer.valueOf(accessor.retropolymorph$getYSize()),
                    Integer.valueOf(this.button.x),
                    Integer.valueOf(this.button.y));
        }
    }

    public GuiButton getButton() {
        return this.button;
    }

    public boolean isVisible() {
        return this.button.visible;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public int getButtonX() {
        return this.button.x;
    }

    public int getButtonY() {
        return this.button.y;
    }

    public int getButtonWidth() {
        return this.button.width;
    }

    public int getButtonHeight() {
        return this.button.height;
    }

    public int getPanelLeft() {
        return this.layout.getPanelLeft();
    }

    public int getPanelTop() {
        return this.layout.getPanelTop();
    }

    public int getPanelWidth() {
        return this.layout.getPanelRight() - this.layout.getPanelLeft();
    }

    public int getPanelHeight() {
        return this.layout.getPanelBottom() - this.layout.getPanelTop();
    }

    public boolean owns(GuiContainer currentGui) {
        return this.gui == currentGui;
    }

    public boolean isRightClickClearEnabled() {
        return this.rightClickClears;
    }

    void update() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            hide();
            return;
        }

        if (mc.player.openContainer != this.context.getContainer()) {
            hide();
            return;
        }

        if (!this.context.getSelectorPlacement().isVisible()) {
            hide();
            return;
        }

        int windowId = this.context.getContainer().windowId;
        boolean windowChanged = ClientSelectionTracker.rebindWindowId(
                windowId, this.sessionToken);
        if (windowChanged) {
            LOGGER.debug(
                    "Selector window rebound: container={}, session={}, window={}",
                    this.context.getContainer().getClass().getName(),
                    Integer.valueOf(this.sessionToken),
                    Integer.valueOf(windowId));
        }

        boolean inputsChanged = this.cache.refreshInputs(this.context);
        if (windowChanged || inputsChanged) {
            this.inputRevision = nextInputRevision(this.inputRevision);
            ClientSelectionTracker.expectInputRevision(
                    windowId, this.sessionToken, this.inputRevision);
            // A successful craft changes stack counts one tick before the server's
            // refreshed recipe list returns. Clearing choices here hid the selector
            // during that round-trip, which looked like a button flicker on every
            // WCT/AE2 craft. Keep the last authoritative choices visible for ordinary
            // input changes; a real window rebind must still discard stale choices.
            if (windowChanged) {
                this.cache.clearChoices();
            }
            this.layout.resetPage();
            this.navigation.reset();
            this.expanded = false;
            SelectorQueryScheduler.scheduleQuery(windowId, this.sessionToken, this.inputRevision);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Selector query: container={}, context={}, revision={}, inputs={}",
                        this.context.getContainer().getClass().getName(),
                        this.context.getClass().getName(),
                        Integer.valueOf(this.inputRevision),
                        describeInputs());
            }
        }

        long selectionRevision = ClientSelectionTracker.getRevision(windowId, this.sessionToken);
        boolean authoritativeUpdate = selectionRevision != this.appliedSelectionRevision;
        if (authoritativeUpdate) {
            String selected = ClientSelectionTracker.getSelectedRecipeKey(
                    windowId, this.sessionToken);
            this.context.applyRemoteSelection(selected);
            boolean choicesChanged = this.cache.setChoices(
                    ClientSelectionTracker.getOptions(windowId, this.sessionToken),
                    selected);
            this.appliedSelectionRevision = selectionRevision;

            if (choicesChanged) {
                this.layout.resetPage();
                this.navigation.reset();
                this.expanded = false;
            }

            List<RecipeOption> choices = this.cache.getChoices();
            JeiTransferIntent.tryApply(
                    this.context, choices, this.sessionToken, this.inputRevision);
            LOGGER.debug(
                    "Selector sync: container={}, revision={}, selected={}, matches={}, selectorVisible={}",
                    this.context.getContainer().getClass().getName(),
                    Integer.valueOf(this.inputRevision),
                    selected,
                    Integer.valueOf(choices.size()),
                    Boolean.valueOf(choices.size() > 1));
        }

        ensureButtonAttached();
        updateButtonPosition();
        List<RecipeOption> choices = this.cache.getChoices();
        this.button.visible = choices.size() > 1 && this.context.getSelectorPlacement().isVisible();
        String selected = ClientSelectionTracker.getSelectedRecipeKey(windowId, this.sessionToken);
        boolean error = !ClientSelectionTracker.wasLastAccepted(windowId, this.sessionToken);
        this.button.setState(selected != null, error);

        if (!this.button.visible) {
            this.expanded = false;
            this.navigation.reset();
        }
    }

    void drawOverlay(int mouseX, int mouseY) {
        if (!this.button.visible) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        int windowId = this.context.getContainer().windowId;
        String selected = ClientSelectionTracker.getSelectedRecipeKey(windowId, this.sessionToken);
        SelectionReason reason = ClientSelectionTracker.getLastReason(windowId, this.sessionToken);
        if (this.expanded) {
            List<RecipeOption> choices = this.cache.getChoices();
            updateLayout(choices.size());
            this.renderer.drawPanel(
                    mc,
                    this.gui,
                    this.layout,
                    this.navigation,
                    choices,
                    selected,
                    reason,
                    mouseX,
                    mouseY);
        }

        if (contains(
                mouseX,
                mouseY,
                this.button.x,
                this.button.y,
                this.button.width,
                this.button.height)) {
            this.renderer.drawButtonTooltip(
                    mc,
                    this.gui,
                    this.cache.getChoices().size(),
                    ClientSelectionTracker.wasLastAccepted(windowId, this.sessionToken),
                    selected,
                    reason,
                    this.rightClickClears,
                    this.wheelCyclesButton,
                    this.expanded,
                    mouseX,
                    mouseY);
        }
    }

    void toggle() {
        if (!this.button.visible) {
            return;
        }

        this.expanded = !this.expanded;
        if (this.expanded) {
            List<RecipeOption> choices = this.cache.getChoices();
            updateLayout(choices.size());
            String selected = ClientSelectionTracker.getSelectedRecipeKey(
                    this.context.getContainer().windowId,
                    this.sessionToken);
            int selectedIndex = indexOfRecipeKey(choices, selected);
            this.navigation.focusSelectedOrFirst(choices.size(), selectedIndex);
            this.layout.ensureVisible(this.navigation.getFocusedIndex());
        } else {
            this.navigation.reset();
        }
    }

    boolean handleKeyboardInput(int keyCode) {
        if (!this.expanded || !this.button.visible) {
            return false;
        }

        List<RecipeOption> choices = this.cache.getChoices();
        if (choices.isEmpty()) {
            this.expanded = false;
            this.navigation.reset();
            return false;
        }
        updateLayout(choices.size());

        SelectorAction action = SelectorInteractionHandler.translateKeyboard(
                keyCode,
                this.expanded,
                this.button.visible,
                choices.size(),
                this.layout.getVisibleCount(),
                this.layout.getStartIndex(),
                this.layout.getEndIndex());

        if (action.getType() != SelectorAction.Type.NONE) {
            executeAction(action, choices);
            return true;
        }
        return false;
    }

    boolean handleMouseInput(int mouseX, int mouseY, int mouseButton, boolean pressed, int wheel) {
        if (!this.button.visible) {
            return false;
        }

        List<RecipeOption> choices = this.cache.getChoices();
        if (this.expanded) {
            updateLayout(choices.size());
        }

        SelectorAction action = SelectorInteractionHandler.translateMouse(
                mouseX,
                mouseY,
                mouseButton,
                pressed,
                wheel,
                this.button.visible,
                this.expanded,
                this.wheelCyclesButton,
                this.rightClickClears,
                this.layout,
                this.button.x,
                this.button.y,
                this.button.width,
                this.button.height,
                choices.size());

        if (action.getType() != SelectorAction.Type.NONE) {
            executeAction(action, choices);
            return true;
        }

        if (this.expanded && (wheel != 0 || pressed)) {
            if (this.layout.isInsidePanel(mouseX, mouseY)
                    || contains(mouseX, mouseY, this.button.x, this.button.y, this.button.width, this.button.height)) {
                return true;
            }
        }
        return false;
    }

    private void executeAction(SelectorAction action, List<RecipeOption> choices) {
        switch (action.getType()) {
            case TOGGLE:
                toggle();
                break;
            case CLOSE:
                this.expanded = false;
                this.navigation.reset();
                break;
            case CLEAR_TO_AUTO:
                clearSelection();
                break;
            case CYCLE_SELECTION:
                cycleSelection(action.getValue());
                break;
            case MOVE_FOCUS:
                moveKeyboardFocus(action.getValue(), choices.size());
                break;
            case FOCUS_ABSOLUTE:
                focusAbsolute(action.getValue(), choices.size());
                break;
            case SELECT_FOCUSED:
                selectFocusedChoice(choices);
                break;
            case SELECT_INDEX:
                int index = action.getValue();
                this.navigation.setFocusedIndex(index, choices.size());
                selectChoice(choices, index);
                break;
            case SCROLL_PANEL:
                this.layout.moveOffset(action.getValue());
                this.navigation.keepInsideVisibleRange(
                        this.layout.getStartIndex(), this.layout.getEndIndex(), choices.size());
                break;
            case PAGE_LEFT:
                this.layout.pageLeft();
                this.navigation.keepInsideVisibleRange(
                        this.layout.getStartIndex(), this.layout.getEndIndex(), choices.size());
                playSelectionSound(Minecraft.getMinecraft());
                break;
            case PAGE_RIGHT:
                this.layout.pageRight();
                this.navigation.keepInsideVisibleRange(
                        this.layout.getStartIndex(), this.layout.getEndIndex(), choices.size());
                playSelectionSound(Minecraft.getMinecraft());
                break;
            case NONE:
            default:
                break;
        }
    }

    private void cycleSelection(int delta) {
        List<RecipeOption> choices = this.cache.getChoices();
        if (choices.size() <= 1) {
            return;
        }
        String selected = ClientSelectionTracker.getSelectedRecipeKey(
                this.context.getContainer().windowId, this.sessionToken);
        int target = this.navigation.cycleFromSelection(
                indexOfRecipeKey(choices, selected), delta, choices.size());
        if (target >= 0) {
            selectChoice(choices, target);
        }
    }

    private void moveKeyboardFocus(int delta, int choiceCount) {
        if (this.navigation.moveBy(delta, choiceCount)) {
            playSelectionSound(Minecraft.getMinecraft());
        }
        this.layout.ensureVisible(this.navigation.getFocusedIndex());
    }

    private void focusAbsolute(int index, int choiceCount) {
        if (this.navigation.setFocusedIndex(index, choiceCount)) {
            playSelectionSound(Minecraft.getMinecraft());
        }
        this.layout.ensureVisible(this.navigation.getFocusedIndex());
    }

    private void selectFocusedChoice(List<RecipeOption> choices) {
        int focused = this.navigation.getFocusedIndex();
        if (focused < 0 || focused >= choices.size()) {
            this.navigation.focusSelectedOrFirst(choices.size(), -1);
            focused = this.navigation.getFocusedIndex();
        }
        selectChoice(choices, focused);
    }

    private void selectChoice(List<RecipeOption> choices, int choiceIndex) {
        if (choiceIndex < 0 || choiceIndex >= choices.size()) {
            return;
        }
        RecipeOption choice = choices.get(choiceIndex);
        ClientSelectionTracker.applyOptimisticSelection(
                this.context.getContainer().windowId,
                this.sessionToken,
                choice.getRecipeKey());
        this.context.applyRemoteSelection(choice.getRecipeKey());
        SelectorQueryScheduler.cancel();
        NetworkHandler.select(
                this.context.getContainer().windowId,
                this.sessionToken,
                this.inputRevision,
                choice.getRecipeKey());
        playSelectionSound(Minecraft.getMinecraft());
        if (this.closeAfterSelection) {
            this.expanded = false;
            this.navigation.reset();
        }
    }

    void clearSelection() {
        this.expanded = false;
        this.navigation.reset();
        ClientSelectionTracker.applyOptimisticSelection(
                this.context.getContainer().windowId, this.sessionToken, null);
        this.context.applyRemoteSelection(null);
        SelectorQueryScheduler.cancel();
        NetworkHandler.clear(
                this.context.getContainer().windowId,
                this.sessionToken,
                this.inputRevision);
        playSelectionSound(Minecraft.getMinecraft());
    }

    private void hide() {
        this.button.visible = false;
        this.expanded = false;
        this.navigation.reset();
    }

    private void ensureButtonAttached() {
        List<GuiButton> buttons = ((GuiScreenAccessor) this.gui).retropolymorph$getButtonList();
        if (buttons.contains(this.button)) {
            return;
        }

        buttons.add(this.button);
        if (!this.buttonReattachLogged) {
            this.buttonReattachLogged = true;
            LOGGER.debug(
                    "Selector button reattached after GUI button-list rebuild: gui={}, container={}",
                    this.gui.getClass().getName(),
                    this.context.getContainer().getClass().getName());
        }
    }

    private void updateButtonPosition() {
        SelectorPlacement placement = this.context.getSelectorPlacement();
        if (!placement.isVisible()) {
            return;
        }

        SelectorPlacementResolver.Position pos = SelectorPlacementResolver.resolveButtonPosition(
                this.gui,
                placement,
                this.buttonOffsetX,
                this.buttonOffsetY,
                PolymorphButton.BUTTON_SIZE);
        this.button.x = pos.x;
        this.button.y = pos.y;
    }

    private void updateLayout(int choiceCount) {
        this.layout.update(
                this.gui.width,
                this.gui.height,
                this.button.x,
                this.button.y,
                this.button.width,
                this.button.height,
                choiceCount);
    }

    private static void playSelectionSound(Minecraft mc) {
        if (mc != null && mc.getSoundHandler() != null) {
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static int indexOfRecipeKey(List<RecipeOption> choices, String recipeKey) {
        if (recipeKey == null || choices == null) {
            return -1;
        }
        for (int index = 0; index < choices.size(); index++) {
            RecipeOption option = choices.get(index);
            if (option != null && recipeKey.equals(option.getRecipeKey())) {
                return index;
            }
        }
        return -1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private String describeInputs() {
        StringBuilder inputs = new StringBuilder();
        for (int index = 0; index < this.context.getInputCount(); index++) {
            ItemStack stack = this.context.getInputStack(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (inputs.length() > 0) {
                inputs.append(';');
            }
            inputs.append(index).append('=')
                    .append(stack.getItem().getRegistryName())
                    .append('@').append(stack.getMetadata())
                    .append('x').append(stack.getCount());
        }
        return inputs.length() == 0 ? "empty" : inputs.toString();
    }


    private static int nextInputRevision(int current) {
        return current == Integer.MAX_VALUE ? 1 : current + 1;
    }
    private static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
