package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.mixin.GuiContainerAccessor;
import dev.sosea1.retropolymorph.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public final class RecipeSelectorController {

    private static final int BUTTON_ID = 0x504345;

    private static final ResourceLocation SPRITE_OUTPUT =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/output_button.png");
    private static final ResourceLocation SPRITE_OUTPUT_HIGHLIGHTED =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/output_button_highlighted.png");
    private static final ResourceLocation SPRITE_CURRENT_OUTPUT =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/current_output.png");
    private static final ResourceLocation SPRITE_CURRENT_OUTPUT_HIGHLIGHTED =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/current_output_highlighted.png");

    private final GuiContainer gui;
    private final SelectionContext context;
    private final int sessionToken;
    private final PolymorphButton button;
    private final ClientRecipeCache cache = new ClientRecipeCache();
    private final SelectorLayout layout;
    private final ArrayList<String> tooltipLines = new ArrayList<String>(4);

    private final int buttonOffsetX;
    private final int buttonOffsetY;
    private final boolean showRecipeKeyInTooltip;
    private final boolean rightClickClears;
    private final boolean closeAfterSelection;

    private boolean expanded;
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
        this.layout = new SelectorLayout(
                PolymorphConfig.getSelectorColumns(),
                PolymorphConfig.getSelectorRows());

        this.buttonOffsetX = PolymorphConfig.getButtonOffsetX();
        this.buttonOffsetY = PolymorphConfig.getButtonOffsetY();
        this.showRecipeKeyInTooltip = PolymorphConfig.isShowRecipeKeyInTooltip();
        this.rightClickClears = PolymorphConfig.isRightClickClears();
        this.closeAfterSelection = PolymorphConfig.isCloseAfterSelection();

        this.appliedSelectionRevision = ClientSelectionTracker.getRevision(
                context.getContainer().windowId,
                sessionToken);
        updateButtonPosition();
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

        int windowId = this.context.getContainer().windowId;
        long selectionRevision = ClientSelectionTracker.getRevision(windowId, this.sessionToken);
        boolean authoritativeUpdate = selectionRevision != this.appliedSelectionRevision;
        if (authoritativeUpdate) {
            this.context.applyRemoteSelection(
                    ClientSelectionTracker.getSelectedRecipeKey(windowId, this.sessionToken));
            if (!ClientSelectionTracker.wasLastAccepted(windowId, this.sessionToken)) {
                this.cache.invalidate();
            }
            this.appliedSelectionRevision = selectionRevision;
        }

        boolean changed = this.cache.refresh(this.context, mc.world);
        List<RecipeOption> choices = this.cache.getChoices();

        if (changed) {
            this.layout.resetPage();
            this.expanded = false;
            if (!authoritativeUpdate && ClientSelectionTracker.reconcile(
                    windowId,
                    this.sessionToken,
                    choices,
                    this.context.retainSelectionWhenOptionsEmpty())) {
                NetworkHandler.query(windowId, this.sessionToken);
            }
            JeiTransferIntent.tryApply(this.context, choices, this.sessionToken);
        }

        updateButtonPosition();
        this.button.visible = choices.size() > 1;
        String selected = ClientSelectionTracker.getSelectedRecipeKey(windowId, this.sessionToken);
        boolean error = !ClientSelectionTracker.wasLastAccepted(windowId, this.sessionToken);
        this.button.setState(selected != null, error);

        if (!this.button.visible) {
            this.expanded = false;
        }
    }

    void drawOverlay(int mouseX, int mouseY) {
        if (!this.button.visible) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (this.expanded) {
            drawPanel(mc, this.cache.getChoices(), mouseX, mouseY);
        }

        if (contains(
                mouseX,
                mouseY,
                this.button.x,
                this.button.y,
                this.button.width,
                this.button.height)) {
            drawButtonTooltip(mc, mouseX, mouseY);
        }
    }

    void toggle() {
        if (!this.button.visible) {
            return;
        }

        this.expanded = !this.expanded;
        if (this.expanded) {
            updateLayout(this.cache.getChoices().size());
        }
    }

    boolean handleMouseInput(int mouseX, int mouseY, int mouseButton, boolean pressed, int wheel) {
        if (!this.expanded || !this.button.visible) {
            return false;
        }

        List<RecipeOption> choices = this.cache.getChoices();
        updateLayout(choices.size());
        boolean insidePanel = this.layout.isInsidePanel(mouseX, mouseY);

        if (wheel != 0) {
            if (!insidePanel) {
                return false;
            }

            this.layout.moveOffset(wheel < 0 ? 1 : -1);
            return true;
        }

        if (!pressed) {
            return false;
        }

        if (mouseButton == 0 && contains(
                mouseX,
                mouseY,
                this.button.x,
                this.button.y,
                this.button.width,
                this.button.height)) {
            return false;
        }

        if (!insidePanel) {
            if (mouseButton == 0) {
                this.expanded = false;
            }
            return false;
        }

        if (mouseButton != 0) {
            return true;
        }

        if (this.layout.isLeftArrow(mouseX, mouseY)) {
            this.layout.moveOffset(-1);
            return true;
        } else if (this.layout.isRightArrow(mouseX, mouseY)) {
            this.layout.moveOffset(1);
            return true;
        }

        int choiceIndex = this.layout.choiceAt(mouseX, mouseY);
        if (choiceIndex >= 0 && choiceIndex < choices.size()) {
            RecipeOption choice = choices.get(choiceIndex);
            ClientSelectionTracker.apply(
                    this.context.getContainer().windowId,
                    this.sessionToken,
                    true,
                    choice.getRecipeKey());
            this.context.applyRemoteSelection(choice.getRecipeKey());
            NetworkHandler.select(
                    this.context.getContainer().windowId,
                    this.sessionToken,
                    choice.getRecipeKey());
            if (this.closeAfterSelection) {
                this.expanded = false;
            }
        }
        return true;
    }

    void clearSelection() {
        this.expanded = false;
        NetworkHandler.clear(this.context.getContainer().windowId, this.sessionToken);
    }

    private void hide() {
        this.button.visible = false;
        this.expanded = false;
    }

    private void updateButtonPosition() {
        GuiContainerAccessor accessor = (GuiContainerAccessor) this.gui;
        Slot result = this.context.getResultSlot();

        int x = accessor.retropolymorph$getGuiLeft() + result.xPos + (16 - PolymorphButton.BUTTON_SIZE) / 2;
        int y = accessor.retropolymorph$getGuiTop() + result.yPos - 22;

        x += this.buttonOffsetX + this.context.getButtonOffsetX();
        y += this.buttonOffsetY + this.context.getButtonOffsetY();

        this.button.x = clamp(x, 2, Math.max(2, this.gui.width - PolymorphButton.BUTTON_SIZE - 2));
        this.button.y = clamp(y, 2, Math.max(2, this.gui.height - PolymorphButton.BUTTON_SIZE - 2));
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

    private void drawPanel(Minecraft mc, List<RecipeOption> choices, int mouseX, int mouseY) {
        updateLayout(choices.size());
        int start = this.layout.getStartIndex();
        int end = this.layout.getEndIndex();
        String selected = ClientSelectionTracker.getSelectedRecipeKey(
                this.context.getContainer().windowId,
                this.sessionToken);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        for (int index = start; index < end; index++) {
            int cellX = this.layout.getCellX(index);
            int cellY = this.layout.getCellY(index);

            RecipeOption choice = choices.get(index);
            boolean isSelected = selected != null && selected.equals(choice.getRecipeKey());
            boolean hovered = contains(
                    mouseX,
                    mouseY,
                    cellX,
                    cellY,
                    SelectorLayout.CELL_SIZE,
                    SelectorLayout.CELL_SIZE);

            ResourceLocation sprite;
            if (isSelected) {
                sprite = hovered ? SPRITE_CURRENT_OUTPUT_HIGHLIGHTED : SPRITE_CURRENT_OUTPUT;
            } else {
                sprite = hovered ? SPRITE_OUTPUT_HIGHLIGHTED : SPRITE_OUTPUT;
            }

            mc.getTextureManager().bindTexture(sprite);
            Gui.drawModalRectWithCustomSizedTexture(cellX, cellY, 0, 0, 25, 25, 25, 25);

            ItemStack output = choice.getOutput();
            if (!output.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableDepth();
                mc.getRenderItem().renderItemAndEffectIntoGUI(output, cellX + 4, cellY + 4);
                mc.getRenderItem().renderItemOverlays(mc.fontRenderer, output, cellX + 4, cellY + 4);
                RenderHelper.disableStandardItemLighting();
            } else {
                mc.fontRenderer.drawString("?", cellX + 9, cellY + 8, 0xFFFFFF);
            }
        }

        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.layout.hasNavigation()) {
            int pLeft = this.layout.getPanelLeft();
            int pTop = this.layout.getPanelTop();
            int pRight = this.layout.getPanelRight();
            int pBottom = this.layout.getPanelBottom();

            boolean leftHover = this.layout.isLeftArrow(mouseX, mouseY);
            boolean rightHover = this.layout.isRightArrow(mouseX, mouseY);

            // Left navigation arrow button
            int leftBg = leftHover ? 0xFFD8D8D8 : 0xFFC6C6C6;
            Gui.drawRect(pLeft, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, leftBg);
            Gui.drawRect(pLeft, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pTop + 1, 0xFFFFFFFF);
            Gui.drawRect(pLeft, pTop, pLeft + 1, pBottom, 0xFFFFFFFF);
            Gui.drawRect(pLeft, pBottom - 1, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, 0xFF555555);
            Gui.drawRect(pLeft + SelectorLayout.NAV_ARROW_WIDTH - 1, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, 0xFF555555);
            Gui.drawRect(pLeft, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pTop + 1, 0xFF000000);
            Gui.drawRect(pLeft, pBottom - 1, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, 0xFF000000);
            Gui.drawRect(pLeft, pTop, pLeft + 1, pBottom, 0xFF000000);
            mc.fontRenderer.drawStringWithShadow("<", pLeft + 4, pTop + 8, leftHover ? 0xFFFFA0 : 0xFFFFFFFF);

            // Right navigation arrow button
            int rightBg = rightHover ? 0xFFD8D8D8 : 0xFFC6C6C6;
            Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight, pBottom, rightBg);
            Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight, pTop + 1, 0xFFFFFFFF);
            Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight - SelectorLayout.NAV_ARROW_WIDTH + 1, pBottom, 0xFFFFFFFF);
            Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pBottom - 1, pRight, pBottom, 0xFF555555);
            Gui.drawRect(pRight - 1, pTop, pRight, pBottom, 0xFF555555);
            Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight, pTop + 1, 0xFF000000);
            Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pBottom - 1, pRight, pBottom, 0xFF000000);
            Gui.drawRect(pRight - 1, pTop, pRight, pBottom, 0xFF000000);
            mc.fontRenderer.drawStringWithShadow(">", pRight - 8, pTop + 8, rightHover ? 0xFFFFA0 : 0xFFFFFFFF);
        }

        int hoveredIndex = this.layout.choiceAt(mouseX, mouseY);
        if (hoveredIndex >= 0 && hoveredIndex < choices.size()) {
            RecipeOption hovered = choices.get(hoveredIndex);
            drawRecipeTooltip(mc, hovered, selected, mouseX, mouseY);
        }
    }

    private void drawRecipeTooltip(
            Minecraft mc,
            RecipeOption choice,
            String selected,
            int mouseX,
            int mouseY) {
        ItemStack output = choice.getOutput();
        this.tooltipLines.clear();
        this.tooltipLines.add(output.isEmpty() ? "<empty output>" : output.getDisplayName());
        if (selected != null && selected.equals(choice.getRecipeKey())) {
            this.tooltipLines.add("\u00a7a" + I18n.format("retropolymorph.selector.selected"));
        }
        if (this.showRecipeKeyInTooltip) {
            this.tooltipLines.add("\u00a78" + choice.getRecipeKey());
        }
        GuiUtils.drawHoveringText(
                output,
                this.tooltipLines,
                mouseX,
                mouseY,
                this.gui.width,
                this.gui.height,
                -1,
                mc.fontRenderer);
    }

    private void drawButtonTooltip(Minecraft mc, int mouseX, int mouseY) {
        int windowId = this.context.getContainer().windowId;
        String selected = ClientSelectionTracker.getSelectedRecipeKey(windowId, this.sessionToken);

        this.tooltipLines.clear();
        this.tooltipLines.add(I18n.format(
                "retropolymorph.selector.tooltip",
                Integer.valueOf(this.cache.getChoices().size())));
        if (!ClientSelectionTracker.wasLastAccepted(windowId, this.sessionToken)) {
            this.tooltipLines.add("\u00a7c" + I18n.format("retropolymorph.selector.rejected"));
        } else if (selected != null) {
            this.tooltipLines.add("\u00a7a" + I18n.format("retropolymorph.selector.selected"));
        }
        if (this.rightClickClears && selected != null) {
            this.tooltipLines.add("\u00a77" + I18n.format("retropolymorph.selector.reset"));
        }
        GuiUtils.drawHoveringText(
                ItemStack.EMPTY,
                this.tooltipLines,
                mouseX,
                mouseY,
                this.gui.width,
                this.gui.height,
                -1,
                mc.fontRenderer);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
