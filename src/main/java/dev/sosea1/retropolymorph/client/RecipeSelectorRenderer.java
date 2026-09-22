package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionReason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Foreground rendering and tooltip presentation for the selector.
 *
 * <p>Keeping this separate from {@link RecipeSelectorController} prevents the
 * networking/input coordinator from becoming the owner of OpenGL state and
 * presentation-only recipe metadata.</p>
 */
final class RecipeSelectorRenderer {

    private static final ResourceLocation SPRITE_OUTPUT =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/output_button.png");
    private static final ResourceLocation SPRITE_OUTPUT_HIGHLIGHTED =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/output_button_highlighted.png");
    private static final ResourceLocation SPRITE_CURRENT_OUTPUT =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/current_output.png");
    private static final ResourceLocation SPRITE_CURRENT_OUTPUT_HIGHLIGHTED =
            new ResourceLocation("retropolymorph", "textures/gui/sprites/current_output_highlighted.png");

    private final ArrayList<String> tooltipLines = new ArrayList<String>(6);
    private final boolean showRecipeKeyInTooltip;
    private final boolean showRecipeSourceInTooltip;

    RecipeSelectorRenderer(boolean showRecipeKeyInTooltip, boolean showRecipeSourceInTooltip) {
        this.showRecipeKeyInTooltip = showRecipeKeyInTooltip;
        this.showRecipeSourceInTooltip = showRecipeSourceInTooltip;
    }

    void drawPanel(
            Minecraft mc,
            GuiContainer gui,
            SelectorLayout layout,
            SelectorNavigationState navigation,
            List<RecipeOption> choices,
            String selected,
            SelectionReason reason,
            int mouseX,
            int mouseY) {
        int start = layout.getStartIndex();
        int end = layout.getEndIndex();

        /*
         * Some legacy machine GUIs (notably IC2's Industrial Workbench) leave
         * slot/tool geometry in the depth buffer or draw late post-GUI layers.
         * Render the selector as one isolated, foreground GUI layer instead of
         * merely painting an opaque rectangle at the machine's original Z.
         */
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 400.0F);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();

        // Selector sprites have transparent edges. Cover the machine GUI first so
        // slots and tool icons below the expanded panel cannot bleed through.
        Gui.drawRect(
                layout.getPanelLeft(),
                layout.getPanelTop(),
                layout.getPanelRight(),
                layout.getPanelBottom(),
                0xFFC6C6C6);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        for (int index = start; index < end; index++) {
            int cellX = layout.getCellX(index);
            int cellY = layout.getCellY(index);

            RecipeOption choice = choices.get(index);
            boolean isSelected = selected != null && selected.equals(choice.getRecipeKey());
            boolean hovered = contains(
                    mouseX,
                    mouseY,
                    cellX,
                    cellY,
                    SelectorLayout.CELL_SIZE,
                    SelectorLayout.CELL_SIZE);
            boolean keyboardFocused = navigation.getFocusedIndex() == index;
            boolean highlighted = hovered || keyboardFocused;

            ResourceLocation sprite;
            if (isSelected) {
                sprite = highlighted ? SPRITE_CURRENT_OUTPUT_HIGHLIGHTED : SPRITE_CURRENT_OUTPUT;
            } else {
                sprite = highlighted ? SPRITE_OUTPUT_HIGHLIGHTED : SPRITE_OUTPUT;
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
                GlStateManager.disableDepth();
            } else {
                mc.fontRenderer.drawString("?", cellX + 9, cellY + 8, 0xFFFFFF);
            }
        }

        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (layout.hasNavigation()) {
            drawNavigation(mc, layout, mouseX, mouseY);
        }

        // Restore ordinary GUI state before tooltips or any later event handlers.
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        GlStateManager.enableDepth();

        int hoveredIndex = layout.choiceAt(mouseX, mouseY);
        if (hoveredIndex >= 0 && hoveredIndex < choices.size()) {
            drawRecipeTooltip(mc, gui, choices.get(hoveredIndex), selected, reason, mouseX, mouseY);
        }
    }

    void drawPanel(
            Minecraft mc,
            GuiContainer gui,
            SelectorLayout layout,
            SelectorNavigationState navigation,
            List<RecipeOption> choices,
            String selected,
            int mouseX,
            int mouseY) {
        drawPanel(mc, gui, layout, navigation, choices, selected, SelectionReason.NATIVE_DEFAULT, mouseX, mouseY);
    }

    void drawButtonTooltip(
            Minecraft mc,
            GuiContainer gui,
            int choiceCount,
            boolean lastAccepted,
            String selected,
            SelectionReason reason,
            boolean rightClickClears,
            boolean wheelCyclesButton,
            boolean expanded,
            int mouseX,
            int mouseY) {
        this.tooltipLines.clear();
        this.tooltipLines.add(I18n.format(
                "retropolymorph.selector.tooltip",
                Integer.valueOf(choiceCount)));
        if (!lastAccepted) {
            this.tooltipLines.add("\u00a7c" + I18n.format("retropolymorph.selector.rejected"));
        } else if (selected != null) {
            String reasonText = I18n.format(getReasonTranslationKey(reason));
            this.tooltipLines.add("\u00a7a" + I18n.format("retropolymorph.selector.selected") + " \u00a77(" + reasonText + ")");
        }
        if (rightClickClears && selected != null) {
            this.tooltipLines.add("\u00a77" + I18n.format("retropolymorph.selector.help.rmb"));
        }
        if (wheelCyclesButton) {
            this.tooltipLines.add("\u00a77" + I18n.format("retropolymorph.selector.help.wheel"));
        }
        if (expanded) {
            this.tooltipLines.add("\u00a78" + I18n.format("retropolymorph.selector.keyboard"));
        }
        GuiUtils.drawHoveringText(
                ItemStack.EMPTY,
                this.tooltipLines,
                mouseX,
                mouseY,
                gui.width,
                gui.height,
                -1,
                mc.fontRenderer);
    }

    void drawButtonTooltip(
            Minecraft mc,
            GuiContainer gui,
            int choiceCount,
            boolean lastAccepted,
            String selected,
            boolean rightClickClears,
            boolean wheelCyclesButton,
            boolean expanded,
            int mouseX,
            int mouseY) {
        drawButtonTooltip(
                mc, gui, choiceCount, lastAccepted, selected,
                SelectionReason.NATIVE_DEFAULT, rightClickClears, wheelCyclesButton,
                expanded, mouseX, mouseY);
    }

    private static void drawNavigation(Minecraft mc, SelectorLayout layout, int mouseX, int mouseY) {
        int pLeft = layout.getPanelLeft();
        int pTop = layout.getPanelTop();
        int pRight = layout.getPanelRight();
        int pBottom = layout.getPanelBottom();

        boolean canLeft = layout.canMoveLeft();
        boolean canRight = layout.canMoveRight();
        boolean leftHover = canLeft && layout.isLeftArrow(mouseX, mouseY);
        boolean rightHover = canRight && layout.isRightArrow(mouseX, mouseY);

        int leftBg = canLeft ? (leftHover ? 0xFFD8D8D8 : 0xFFC6C6C6) : 0xFFA0A0A0;
        Gui.drawRect(pLeft, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, leftBg);
        Gui.drawRect(pLeft, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pTop + 1, 0xFFFFFFFF);
        Gui.drawRect(pLeft, pTop, pLeft + 1, pBottom, 0xFFFFFFFF);
        Gui.drawRect(pLeft, pBottom - 1, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, 0xFF555555);
        Gui.drawRect(pLeft + SelectorLayout.NAV_ARROW_WIDTH - 1, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, 0xFF555555);
        Gui.drawRect(pLeft, pTop, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pTop + 1, 0xFF000000);
        Gui.drawRect(pLeft, pBottom - 1, pLeft + SelectorLayout.NAV_ARROW_WIDTH, pBottom, 0xFF000000);
        Gui.drawRect(pLeft, pTop, pLeft + 1, pBottom, 0xFF000000);
        int leftTextColor = canLeft ? (leftHover ? 0xFFFFA0 : 0xFFFFFFFF) : 0xFF707070;
        mc.fontRenderer.drawStringWithShadow("<", pLeft + 4, pTop + 8, leftTextColor);

        int rightBg = canRight ? (rightHover ? 0xFFD8D8D8 : 0xFFC6C6C6) : 0xFFA0A0A0;
        Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight, pBottom, rightBg);
        Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight, pTop + 1, 0xFFFFFFFF);
        Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight - SelectorLayout.NAV_ARROW_WIDTH + 1, pBottom, 0xFFFFFFFF);
        Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pBottom - 1, pRight, pBottom, 0xFF555555);
        Gui.drawRect(pRight - 1, pTop, pRight, pBottom, 0xFF555555);
        Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pTop, pRight, pTop + 1, 0xFF000000);
        Gui.drawRect(pRight - SelectorLayout.NAV_ARROW_WIDTH, pBottom - 1, pRight, pBottom, 0xFF000000);
        Gui.drawRect(pRight - 1, pTop, pRight, pBottom, 0xFF000000);
        int rightTextColor = canRight ? (rightHover ? 0xFFFFA0 : 0xFFFFFFFF) : 0xFF707070;
        mc.fontRenderer.drawStringWithShadow(">", pRight - 8, pTop + 8, rightTextColor);
    }

    private void drawRecipeTooltip(
            Minecraft mc,
            GuiContainer gui,
            RecipeOption choice,
            String selected,
            SelectionReason reason,
            int mouseX,
            int mouseY) {
        ItemStack output = choice.getOutput();
        this.tooltipLines.clear();
        if (output.isEmpty()) {
            this.tooltipLines.add("<empty output>");
        } else {
            ITooltipFlag flag = mc.gameSettings.advancedItemTooltips
                    ? ITooltipFlag.TooltipFlags.ADVANCED
                    : ITooltipFlag.TooltipFlags.NORMAL;
            this.tooltipLines.addAll(output.getTooltip(mc.player, flag));
        }
        if (selected != null && selected.equals(choice.getRecipeKey())) {
            String reasonText = I18n.format(getReasonTranslationKey(reason));
            this.tooltipLines.add("\u00a7a" + I18n.format("retropolymorph.selector.selected") + " \u00a77(" + reasonText + ")");
        }
        if (this.showRecipeSourceInTooltip) {
            String sourceName = recipeSourceName(choice.getRecipeKey());
            if (sourceName != null) {
                this.tooltipLines.add("\u00a77" + I18n.format(
                        "retropolymorph.selector.source", sourceName));
            }
        }
        if (this.showRecipeKeyInTooltip) {
            this.tooltipLines.add("\u00a78" + choice.getRecipeKey());
        }
        GuiUtils.drawHoveringText(
                output,
                this.tooltipLines,
                mouseX,
                mouseY,
                gui.width,
                gui.height,
                -1,
                mc.fontRenderer);
    }

    static String getReasonTranslationKey(SelectionReason reason) {
        if (reason == null) {
            return "retropolymorph.selector.reason.native_default";
        }
        switch (reason) {
            case CURRENT_CONTEXT:
                return "retropolymorph.selector.reason.current_context";
            case PLAYER_PREFERENCE:
                return "retropolymorph.selector.reason.player_preference";
            case EXACT_RECIPE_POLICY:
                return "retropolymorph.selector.reason.exact_recipe_policy";
            case MOD_PRIORITY:
                return "retropolymorph.selector.reason.mod_priority";
            case AUTOMATIC_MODDED:
                return "retropolymorph.selector.reason.automatic_modded";
            case PLAYER_SELECTION:
                return "retropolymorph.selector.reason.player_selection";
            case NATIVE_DEFAULT:
            default:
                return "retropolymorph.selector.reason.native_default";
        }
    }

    private static String recipeSourceName(String recipeKey) {
        if (recipeKey == null) {
            return null;
        }
        int separator = recipeKey.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        String namespace = recipeKey.substring(0, separator);
        ModContainer mod = Loader.instance().getIndexedModList().get(namespace);
        return mod == null ? namespace : mod.getName();
    }

    private static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
