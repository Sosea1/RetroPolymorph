package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy.PreferenceDecision;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class SmeltingPreferencePolicyTest {

    private static Item vanillaOutput;
    private static Item thermalOutput;
    private static Item mekanismOutput;

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        vanillaOutput = new Item().setRegistryName("minecraft", "retropolymorph_test_vanilla");
        thermalOutput = new Item().setRegistryName("thermalfoundation", "retropolymorph_test_material");
        mekanismOutput = new Item().setRegistryName("mekanism", "retropolymorph_test_ingot");
    }

    @AfterEach
    public void tearDown() {
        SmeltingPreferencePolicy.configure(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true);
    }

    @Test
    public void preferredModsUseOutputNamespace() {
        SmeltingPreferencePolicy.configure(
                Arrays.asList("mekanism", "thermalfoundation", "*", "minecraft"),
                Collections.<String>emptyList(),
                true);

        RecipeOption vanilla = option("smelt:test:vanilla", vanillaOutput, 0);
        RecipeOption thermal = option("smelt:test:thermal", thermalOutput, 0);
        RecipeOption mekanism = option("smelt:test:mekanism", mekanismOutput, 0);

        PreferenceDecision decision = SmeltingPreferencePolicy.decide(
                Arrays.asList(vanilla, thermal, mekanism));

        assertEquals(mekanism.getRecipeKey(), decision.getRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, decision.getReason());
    }

    @Test
    public void preferredOutputBeatsModPriorityAndSupportsExactMeta() {
        SmeltingPreferencePolicy.configure(
                Arrays.asList("mekanism", "thermalfoundation", "*"),
                Arrays.asList("thermalfoundation:retropolymorph_test_material@7"),
                true);

        RecipeOption mekanism = option("smelt:test:mekanism", mekanismOutput, 0);
        RecipeOption thermalWrongMeta = option("smelt:test:thermal0", thermalOutput, 0);
        RecipeOption thermalExact = option("smelt:test:thermal7", thermalOutput, 7);

        PreferenceDecision decision = SmeltingPreferencePolicy.decide(
                Arrays.asList(mekanism, thermalWrongMeta, thermalExact));

        assertEquals(thermalExact.getRecipeKey(), decision.getRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, decision.getReason());
    }

    @Test
    public void outputSelectorWithoutMetaMatchesAnyMeta() {
        SmeltingPreferencePolicy.configure(
                Collections.<String>emptyList(),
                Arrays.asList("thermalfoundation:retropolymorph_test_material"),
                true);

        RecipeOption vanilla = option("smelt:test:vanilla", vanillaOutput, 0);
        RecipeOption thermal = option("smelt:test:thermal12", thermalOutput, 12);

        PreferenceDecision decision = SmeltingPreferencePolicy.decide(
                Arrays.asList(vanilla, thermal));

        assertEquals(thermal.getRecipeKey(), decision.getRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, decision.getReason());
    }

    @Test
    public void automaticModdedOutputPreferenceCanBeDisabled() {
        RecipeOption vanilla = option("smelt:test:vanilla", vanillaOutput, 0);
        RecipeOption modded = option("smelt:test:thermal", thermalOutput, 0);
        List<RecipeOption> options = Arrays.asList(vanilla, modded);

        SmeltingPreferencePolicy.configure(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true);
        PreferenceDecision enabled = SmeltingPreferencePolicy.decide(options);
        assertEquals(modded.getRecipeKey(), enabled.getRecipeKey());
        assertEquals(SelectionReason.AUTOMATIC_MODDED, enabled.getReason());

        SmeltingPreferencePolicy.configure(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                false);
        PreferenceDecision disabled = SmeltingPreferencePolicy.decide(options);
        assertNull(disabled.getRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, disabled.getReason());
    }

    @Test
    public void parserRejectsInvalidSelectorsAndNormalizesCase() {
        List<String> parsed = SmeltingPreferencePolicy.parsePreferredOutputs(new String[] {
                " ThermalFoundation:RetroPolymorph_Test_Material ",
                "mekanism:retropolymorph_test_ingot@3",
                "mekanism:retropolymorph_test_ingot@3",
                "broken selector",
                "mekanism:retropolymorph_test_ingot@-1"
        });

        assertEquals(Arrays.asList(
                "thermalfoundation:retropolymorph_test_material",
                "mekanism:retropolymorph_test_ingot@3"), parsed);
    }

    private static RecipeOption option(String key, Item item, int meta) {
        return new RecipeOption(key, new ItemStack(item, 1, meta));
    }
}
