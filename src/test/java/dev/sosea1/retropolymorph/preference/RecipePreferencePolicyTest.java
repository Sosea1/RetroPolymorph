package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy.PreferenceDecision;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RecipePreferencePolicyTest {

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
    }

    @AfterEach
    public void tearDown() {
        RecipePreferencePolicy.configure(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true);
        RecipePreferencePolicy.setPriority("mod:api_override", 0);
    }

    @Test
    public void testOrderedPreferredModsWithWildcard() {
        List<String> mods = Arrays.asList("thermalfoundation", "mekanism", "*", "minecraft");
        RecipePreferencePolicy.configure(mods, Collections.<String>emptyList());

        RecipeOption optMc = new RecipeOption("minecraft:iron_block", new ItemStack(Items.IRON_INGOT));
        RecipeOption optEnder = new RecipeOption("enderio:iron_block", new ItemStack(Items.IRON_INGOT));
        RecipeOption optThermal = new RecipeOption("thermalfoundation:iron_block", new ItemStack(Items.IRON_INGOT));
        RecipeOption optMek = new RecipeOption("mekanism:iron_block", new ItemStack(Items.IRON_INGOT));

        // thermalfoundation is bucket 0 -> wins
        PreferenceDecision d1 = RecipePreferencePolicy.decide(Arrays.asList(optMc, optEnder, optThermal, optMek));
        assertEquals("thermalfoundation:iron_block", d1.getRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, d1.getReason());

        // Without thermal, mekanism is bucket 1 -> wins
        PreferenceDecision d2 = RecipePreferencePolicy.decide(Arrays.asList(optMc, optEnder, optMek));
        assertEquals("mekanism:iron_block", d2.getRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, d2.getReason());

        // Without thermal and mekanism, enderio matches wildcard '*' (bucket 2) and beats minecraft (bucket 3)
        PreferenceDecision d3 = RecipePreferencePolicy.decide(Arrays.asList(optMc, optEnder));
        assertEquals("enderio:iron_block", d3.getRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, d3.getReason());
    }

    @Test
    public void testWildcardAtBeginning() {
        List<String> mods = Arrays.asList("*", "minecraft");
        RecipePreferencePolicy.configure(mods, Collections.<String>emptyList());

        RecipeOption optMc = new RecipeOption("minecraft:chest", new ItemStack(Items.APPLE));
        RecipeOption optQuark = new RecipeOption("quark:chest", new ItemStack(Items.APPLE));

        PreferenceDecision decision = RecipePreferencePolicy.decide(Arrays.asList(optMc, optQuark));
        assertEquals("quark:chest", decision.getRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, decision.getReason());
    }

    @Test
    public void testImplicitWildcardAtEnd() {
        // When '*' is omitted, implicit '*' is added at the end
        List<String> mods = Arrays.asList("thermalfoundation", "mekanism");
        RecipePreferencePolicy.configure(mods, Collections.<String>emptyList());

        assertTrue(RecipePreferencePolicy.getPreferredMods().contains("*"));
        assertEquals(2, RecipePreferencePolicy.getPreferredMods().indexOf("*"));

        RecipeOption optEnder = new RecipeOption("enderio:copper", new ItemStack(Items.IRON_INGOT));
        RecipeOption optThermal = new RecipeOption("thermalfoundation:copper", new ItemStack(Items.IRON_INGOT));

        PreferenceDecision decision = RecipePreferencePolicy.decide(Arrays.asList(optEnder, optThermal));
        assertEquals("thermalfoundation:copper", decision.getRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, decision.getReason());
    }

    @Test
    public void testTieInSameBucketPreservesNaturalOrder() {
        List<String> mods = Arrays.asList("*", "minecraft");
        RecipePreferencePolicy.configure(mods, Collections.<String>emptyList());

        RecipeOption optA = new RecipeOption("modA:item", new ItemStack(Items.IRON_INGOT));
        RecipeOption optB = new RecipeOption("modB:item", new ItemStack(Items.IRON_INGOT));

        // Both are unlisted mods falling into bucket 0 (*). No mod priority differentiation.
        PreferenceDecision decision = RecipePreferencePolicy.decide(Arrays.asList(optA, optB));
        assertNull(decision.getRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, decision.getReason());
    }

    @Test
    public void testTieInBestBucketBeatsLowerBucket() {
        List<String> mods = Arrays.asList("*", "minecraft");
        RecipePreferencePolicy.configure(mods, Collections.<String>emptyList());

        RecipeOption optA = new RecipeOption("modA:item", new ItemStack(Items.IRON_INGOT));
        RecipeOption optB = new RecipeOption("modB:item", new ItemStack(Items.IRON_INGOT));
        RecipeOption optMc = new RecipeOption("minecraft:item", new ItemStack(Items.IRON_INGOT));

        // modA and modB are in bucket 0, beating minecraft in bucket 1.
        // Natural order between modA and modB picks modA.
        PreferenceDecision decision = RecipePreferencePolicy.decide(Arrays.asList(optA, optB, optMc));
        assertEquals("modA:item", decision.getRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, decision.getReason());
    }

    @Test
    public void testExactRecipeBeatsModPriority() {
        RecipePreferencePolicy.configure(
                Arrays.asList("thermalfoundation", "*"),
                Arrays.asList("enderio:special_recipe"));

        RecipeOption optThermal = new RecipeOption("thermalfoundation:normal", new ItemStack(Items.IRON_INGOT));
        RecipeOption optEnder = new RecipeOption("enderio:special_recipe", new ItemStack(Items.IRON_INGOT));

        PreferenceDecision decision = RecipePreferencePolicy.decide(Arrays.asList(optThermal, optEnder));
        assertEquals("enderio:special_recipe", decision.getRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, decision.getReason());
    }

    @Test
    public void testApiRegisteredPriorityBeatsConfiguredExact() {
        RecipePreferencePolicy.configure(
                Collections.<String>emptyList(),
                Arrays.asList("mod:configured_exact"));
        RecipePreferencePolicy.setPriority("mod:api_override", 500);

        RecipeOption optConfig = new RecipeOption("mod:configured_exact", new ItemStack(Items.IRON_INGOT));
        RecipeOption optApi = new RecipeOption("mod:api_override", new ItemStack(Items.IRON_INGOT));

        PreferenceDecision decision = RecipePreferencePolicy.decide(Arrays.asList(optConfig, optApi));
        assertEquals("mod:api_override", decision.getRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, decision.getReason());
    }

    @Test
    public void testConfigParserKeepsOpaqueWireSafeRecipeKeys() {
        String[] configEntries = new String[] {
                "mod:plain_one",
                "machine:key=variant",
                "mod:plain_two"
        };
        List<String> parsed = PolymorphConfig.parsePreferredRecipes(configEntries);
        assertNotNull(parsed);
        assertEquals(Arrays.asList("mod:plain_one", "machine:key=variant", "mod:plain_two"), parsed);
    }

    @Test
    public void testNegativePriorityRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RecipePreferencePolicy.setPriority("mod:invalid", -1));
    }

    @Test
    public void testPolicySnapshotIntegrity() {
        RecipePreferencePolicy.configure(
                Arrays.asList("thermalfoundation", "*", "minecraft"),
                Arrays.asList("enderio:alloy_iron", "thermal:ingot"));
        RecipePreferencePolicy.setPriority("mod:api_override", 500);

        RecipePreferencePolicy.PolicySnapshot snapshot = RecipePreferencePolicy.getPolicySnapshot();
        assertNotNull(snapshot);
        assertEquals(Arrays.asList("thermalfoundation", "*", "minecraft"), snapshot.getPreferredMods());
        assertEquals(Arrays.asList("enderio:alloy_iron", "thermal:ingot"), snapshot.getConfiguredExactRecipes());
        assertEquals(Integer.valueOf(500), snapshot.getAddonRecipeOverrides().get("mod:api_override"));
        assertTrue(snapshot.isPreferModdedOverVanilla());

        // Verify immutability
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getPreferredMods().add("illegal"));
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getConfiguredExactRecipes().add("illegal"));
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getAddonRecipeOverrides().put("illegal", 1));
    }
}

