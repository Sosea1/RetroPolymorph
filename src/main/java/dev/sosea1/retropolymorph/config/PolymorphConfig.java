package dev.sosea1.retropolymorph.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Small client-UX configuration surface.
 *
 * The recipe engine deliberately has no user-tunable performance knobs: core
 * behavior should stay deterministic. These values only affect presentation
 * and input gestures on the client.
 */
public final class PolymorphConfig {

    private static final String CATEGORY_SELECTOR = "selector";

    private static boolean selectorEnabled = true;
    private static int selectorColumns = 7;
    private static int selectorRows = 1;
    private static int buttonOffsetX;
    private static int buttonOffsetY;
    private static boolean showRecipeKeyInTooltip = true;
    private static boolean rightClickClears = true;
    private static boolean closeAfterSelection = true;

    private PolymorphConfig() {
    }

    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();

        selectorEnabled = config.getBoolean(
                "enabled",
                CATEGORY_SELECTOR,
                true,
                "Show the recipe selector when a compatible GUI has more than one result.");
        selectorColumns = config.getInt(
                "columns",
                CATEGORY_SELECTOR,
                7,
                2,
                12,
                "Number of recipe cells per selector row.");
        selectorRows = config.getInt(
                "rows",
                CATEGORY_SELECTOR,
                1,
                1,
                6,
                "Maximum number of recipe rows shown on one page.");
        buttonOffsetX = config.getInt(
                "buttonOffsetX",
                CATEGORY_SELECTOR,
                0,
                -200,
                200,
                "Horizontal selector-button offset in GUI pixels.");
        buttonOffsetY = config.getInt(
                "buttonOffsetY",
                CATEGORY_SELECTOR,
                0,
                -200,
                200,
                "Vertical selector-button offset in GUI pixels.");
        showRecipeKeyInTooltip = config.getBoolean(
                "showRecipeKeyInTooltip",
                CATEGORY_SELECTOR,
                true,
                "Show the internal recipe key in recipe tooltips.");
        rightClickClears = config.getBoolean(
                "rightClickClears",
                CATEGORY_SELECTOR,
                true,
                "Right-clicking the selector button returns to the default recipe.");
        closeAfterSelection = config.getBoolean(
                "closeAfterSelection",
                CATEGORY_SELECTOR,
                true,
                "Close the recipe panel after choosing a recipe.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean isSelectorEnabled() {
        return selectorEnabled;
    }

    public static int getSelectorColumns() {
        return selectorColumns;
    }

    public static int getSelectorRows() {
        return selectorRows;
    }

    public static int getButtonOffsetX() {
        return buttonOffsetX;
    }

    public static int getButtonOffsetY() {
        return buttonOffsetY;
    }

    public static boolean isShowRecipeKeyInTooltip() {
        return showRecipeKeyInTooltip;
    }

    public static boolean isRightClickClears() {
        return rightClickClears;
    }

    public static boolean isCloseAfterSelection() {
        return closeAfterSelection;
    }
}
