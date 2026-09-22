package dev.sosea1.retropolymorph.config;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * User configuration surface. Core matching/caching remains deterministic;
 * selector values affect client UX while integration toggles allow a broken or
 * unwanted focused adapter to be disabled without removing the whole mod.
 */
public final class PolymorphConfig {

    private static List<String> preferredMods = Collections.emptyList();
    private static List<String> preferredRecipes = Collections.emptyList();

    private static final String CATEGORY_SELECTOR = "selector";
    private static final String CATEGORY_INTEGRATIONS = "integrations";
    private static final String CATEGORY_POLICY = "policy";

    private static boolean selectorEnabled = true;
    private static SelectorMode selectorMode = SelectorMode.COMPACT;
    private static int buttonOffsetX;
    private static int buttonOffsetY;
    private static boolean showRecipeKeyInTooltip = true;
    private static boolean showRecipeSourceInTooltip = true;
    private static boolean rightClickClears = true;
    private static boolean wheelCyclesButton = true;
    private static boolean closeAfterSelection = true;

    private static boolean integrationAe2 = true;
    private static boolean integrationExtendedCrafting = true;
    private static boolean integrationIc2 = true;
    private static boolean integrationCyclic = true;
    private static boolean integrationRftools = true;
    private static boolean integrationTinkers = true;
    private static boolean integrationEnderIo = true;
    private static boolean integrationThaumcraft = true;
    private static boolean integrationRefinedStorage = true;
    private static boolean integrationMekanism = true;
    private static boolean integrationThermal = true;
    private static boolean integrationRetroSophisticatedBackpacks = true;
    private static boolean integrationExtraUtilities2 = true;
    private static boolean integrationJei = true;

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
        selectorMode = SelectorMode.parse(config.getString(
                "mode",
                CATEGORY_SELECTOR,
                "compact",
                "Selector presentation mode: compact shows five choices with arrows/wheel; "
                        + "classic shows the full authoritative list (up to 15) like upstream Polymorph.",
                new String[] { "compact", "classic" }));
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
        showRecipeSourceInTooltip = config.getBoolean(
                "showRecipeSourceInTooltip",
                CATEGORY_SELECTOR,
                true,
                "Show the owning recipe mod/source in recipe tooltips.");
        rightClickClears = config.getBoolean(
                "rightClickClears",
                CATEGORY_SELECTOR,
                true,
                "Right-clicking the selector button returns to the default recipe.");
        wheelCyclesButton = config.getBoolean(
                "wheelCyclesButton",
                CATEGORY_SELECTOR,
                true,
                "Mouse-wheel over the selector button cycles recipes without opening the panel.");
        closeAfterSelection = config.getBoolean(
                "closeAfterSelection",
                CATEGORY_SELECTOR,
                true,
                "Close the recipe panel after choosing a recipe.");

        integrationAe2 = integration(config, "ae2", true, "Applied Energistics 2 terminals.");
        integrationExtendedCrafting = integration(config, "extendedCrafting", true, "Extended Crafting tables.");
        integrationIc2 = integration(config, "ic2", true, "IndustrialCraft 2 crafting machines.");
        integrationCyclic = integration(config, "cyclic", true, "Cyclic persistent workbench.");
        integrationRftools = integration(config, "rftools", true, "RFTools/RFTools Control crafting surfaces.");
        integrationTinkers = integration(config, "tconstruct", true, "Tinkers' Construct Crafting Station.");
        integrationEnderIo = integration(config, "enderio", true, "Ender IO Crafter ghost recipe and automatic craft path.");
        integrationThaumcraft = integration(config, "thaumcraft", true, "Thaumcraft 6 Arcane Workbench recipe selection.");
        integrationRefinedStorage = integration(config, "refinedStorage", true, "Refined Storage 1.12 Crafting Grid and regular Pattern Grid recipe selection.");
        integrationMekanism = integration(config, "mekanism", true, "Mekanism Formulaic Assemblicator manual 3x3 recipe selection.");
        integrationThermal = integration(config, "thermalExpansion", true, "Thermal Expansion Sequential Fabricator ghost-grid and committed recipe selection.");
        integrationRetroSophisticatedBackpacks = integration(
                config,
                "retroSophisticatedBackpacks",
                true,
                "Retro Sophisticated Backpacks Crafting Upgrade. Uses the backpack's real registered crafting wrapper/output pair.");
        integrationExtraUtilities2 = integration(
                config,
                "extraUtilities2",
                true,
                "Extra Utilities 2 Mechanical/Analog Crafter. Uses a focused 3x3 bridge and machine-owned selected recipe state.");
        integrationJei = integration(
                config,
                "jei",
                true,
                "Just Enough Items (JEI) and Had Enough Items (HEI) recipe transfer and GUI exclusion support.");

        String[] rawPreferredMods = config.getStringList(
                "preferredMods",
                CATEGORY_POLICY,
                new String[0],
                "Ordered mod priority list (UniDict-style). Earlier mods have higher priority.\n"
                        + "Use '*' to represent unlisted mods (e.g. 'thermalfoundation', 'mekanism', '*', 'minecraft').\n"
                        + "If '*' is omitted, unlisted mods are placed at the end of the list.");
        preferredMods = parsePreferredMods(rawPreferredMods);

        String[] rawPreferredRecipes = config.getStringList(
                "preferredRecipes",
                CATEGORY_POLICY,
                new String[0],
                "Ordered recipe priorities. Earlier recipes in this list win over later ones.\n"
                        + "Format: recipe_id (e.g. 'enderio:special_recipe').");
        preferredRecipes = parsePreferredRecipes(rawPreferredRecipes);

        RecipePreferencePolicy.configure(preferredMods, preferredRecipes);

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean isSelectorEnabled() {
        return selectorEnabled;
    }

    public static SelectorMode getSelectorMode() {
        return selectorMode;
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

    public static boolean isShowRecipeSourceInTooltip() {
        return showRecipeSourceInTooltip;
    }

    public static boolean isRightClickClears() {
        return rightClickClears;
    }

    public static boolean isWheelCyclesButton() {
        return wheelCyclesButton;
    }

    public static boolean isCloseAfterSelection() {
        return closeAfterSelection;
    }

    public static boolean isIntegrationAe2Enabled() { return integrationAe2; }
    public static boolean isIntegrationExtendedCraftingEnabled() { return integrationExtendedCrafting; }
    public static boolean isIntegrationIc2Enabled() { return integrationIc2; }
    public static boolean isIntegrationCyclicEnabled() { return integrationCyclic; }
    public static boolean isIntegrationRftoolsEnabled() { return integrationRftools; }
    public static boolean isIntegrationTinkersEnabled() { return integrationTinkers; }
    public static boolean isIntegrationEnderIoEnabled() { return integrationEnderIo; }
    public static boolean isIntegrationThaumcraftEnabled() { return integrationThaumcraft; }
    public static boolean isIntegrationRefinedStorageEnabled() { return integrationRefinedStorage; }
    public static boolean isIntegrationMekanismEnabled() { return integrationMekanism; }
    public static boolean isIntegrationThermalEnabled() { return integrationThermal; }
    public static boolean isIntegrationRetroSophisticatedBackpacksEnabled() { return integrationRetroSophisticatedBackpacks; }
    public static boolean isIntegrationExtraUtilities2Enabled() { return integrationExtraUtilities2; }
    public static boolean isIntegrationJeiEnabled() { return integrationJei; }

    public static List<String> getPreferredMods() {
        return preferredMods;
    }

    public static List<String> getPreferredRecipes() {
        return preferredRecipes;
    }

    public static List<String> parsePreferredMods(String[] values) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }
        ArrayList<String> result = new ArrayList<String>();
        for (String raw : values) {
            if (raw == null) {
                continue;
            }
            String mod = raw.trim().toLowerCase(Locale.ROOT);
            if (!mod.isEmpty() && !result.contains(mod)) {
                result.add(mod);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<String> parsePreferredRecipes(String[] values) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }
        ArrayList<String> result = new ArrayList<String>();
        for (String raw : values) {
            if (raw == null) {
                continue;
            }
            String value = raw.trim();
            if (value.isEmpty() || value.indexOf('=') >= 0) {
                continue;
            }
            if (RecipeKey.isWireSafe(value) && !result.contains(value)) {
                result.add(value);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean integration(Configuration config, String name, boolean defaultValue, String description) {
        return config.getBoolean(name, CATEGORY_INTEGRATIONS, defaultValue, description);
    }
}
