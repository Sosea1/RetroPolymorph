package dev.sosea1.retropolymorph.config;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.core.SelectionCommand;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectionPolicyType;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.core.SelectionService;
import dev.sosea1.retropolymorph.core.SelectionServiceResult;
import dev.sosea1.retropolymorph.preference.ConflictFingerprint;
import dev.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy;
import dev.sosea1.retropolymorph.preference.SmeltingPreferencePolicy;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test verifying the full configuration and policy pipeline:
 * retropolymorph.cfg -> PolymorphConfig.load() -> Recipe/SmeltingPreferencePolicy -> SelectionService -> chosen recipeKey.
 */
public final class ConfigPolicyIntegrationTest {

    @TempDir
    Path tempDir;

    private static Item vanillaSmeltItem;
    private static Item thermalSmeltItem;
    private static Item mekanismSmeltItem;

    private NBTTagCompound playerData;

    @BeforeAll
    public static void setupAll() {
        Bootstrap.register();
        try {
            java.lang.reflect.Field mcHome = Class.forName("net.minecraftforge.fml.relauncher.FMLInjectionData")
                    .getDeclaredField("minecraftHome");
            mcHome.setAccessible(true);
            if (mcHome.get(null) == null) {
                mcHome.set(null, new File("."));
            }
        } catch (Throwable t) {
            System.err.println("Could not initialize FMLInjectionData.minecraftHome: " + t);
        }

        vanillaSmeltItem = new Item().setRegistryName("minecraft", "rp_test_ingot");
        thermalSmeltItem = new Item().setRegistryName("thermalfoundation", "rp_test_material");
        mekanismSmeltItem = new Item().setRegistryName("mekanism", "rp_test_ingot");

        try {
            net.minecraftforge.registries.ForgeRegistry<net.minecraft.item.crafting.IRecipe> reg =
                    (net.minecraftforge.registries.ForgeRegistry<net.minecraft.item.crafting.IRecipe>)
                            net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES;
            if (reg != null) {
                reg.unfreeze();
                registerDummyRecipe(reg, "minecraft", "priority_test");
                registerDummyRecipe(reg, "thermalfoundation", "priority_test");
                registerDummyRecipe(reg, "mekanism", "priority_test");
                registerDummyRecipe(reg, "enderio", "priority_test");
                reg.freeze();
            }
        } catch (Throwable t) {
            System.err.println("Could not register dummy test recipes in ForgeRegistries.RECIPES: " + t);
        }
    }

    private static void registerDummyRecipe(
            net.minecraftforge.registries.ForgeRegistry<net.minecraft.item.crafting.IRecipe> reg,
            String namespace,
            String path) {
        net.minecraft.util.ResourceLocation id = new net.minecraft.util.ResourceLocation(namespace, path);
        if (reg.getValue(id) == null) {
            net.minecraft.item.crafting.ShapelessRecipes recipe = new net.minecraft.item.crafting.ShapelessRecipes(
                    "",
                    new ItemStack(Items.IRON_INGOT),
                    net.minecraft.util.NonNullList.<net.minecraft.item.crafting.Ingredient>create());
            recipe.setRegistryName(id);
            reg.register(recipe);
        }
    }

    @BeforeEach
    public void setup() {
        this.playerData = new NBTTagCompound();
    }

    @AfterEach
    public void tearDown() {
        RecipePreferencePolicy.configure(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true);
        SmeltingPreferencePolicy.configure(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true);
    }

    // =========================================================================
    // 2. Crafting Policy Matrix (C1 - C10)
    // =========================================================================

    @Test
    @DisplayName("C1 — Default config: preferModdedOverVanilla=true selects first modded recipe")
    public void testCraftingC1_DefaultConfig() throws IOException {
        File cfg = writeConfig("c1.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("thermalfoundation:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.AUTOMATIC_MODDED, result.getReason());
        assertTrue(RecipePreferencePolicy.getPolicySnapshot().isPreferModdedOverVanilla());
    }

    @Test
    @DisplayName("C2 — Automatic preference disabled: preferModdedOverVanilla=false preserves native order")
    public void testCraftingC2_AutomaticPreferenceDisabled() throws IOException {
        File cfg = writeConfig("c2.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=false\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertNull(result.getSelectedRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, result.getReason());
        assertFalse(RecipePreferencePolicy.getPolicySnapshot().isPreferModdedOverVanilla());
    }

    @Test
    @DisplayName("C3 — Mekanism first in preferredMods")
    public void testCraftingC3_MekanismFirst() throws IOException {
        File cfg = writeConfig("c3.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        thermalfoundation\n" +
                "        *\n" +
                "        minecraft\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("mekanism:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
        assertEquals(Arrays.asList("mekanism", "thermalfoundation", "*", "minecraft"),
                RecipePreferencePolicy.getPolicySnapshot().getPreferredMods());
    }

    @Test
    @DisplayName("C4 — Thermal first in preferredMods")
    public void testCraftingC4_ThermalFirst() throws IOException {
        File cfg = writeConfig("c4.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "    S:preferredMods <\n" +
                "        thermalfoundation\n" +
                "        mekanism\n" +
                "        *\n" +
                "        minecraft\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("thermalfoundation:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
    }

    @Test
    @DisplayName("C5 — Vanilla explicitly first in preferredMods")
    public void testCraftingC5_VanillaExplicitlyFirst() throws IOException {
        File cfg = writeConfig("c5.cfg",
                "policy {\n" +
                "    S:preferredMods <\n" +
                "        minecraft\n" +
                "        *\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("minecraft:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
    }

    @Test
    @DisplayName("C6 — Wildcard before vanilla selects first non-Minecraft recipe from natural order")
    public void testCraftingC6_WildcardBeforeVanilla() throws IOException {
        File cfg = writeConfig("c6.cfg",
                "policy {\n" +
                "    S:preferredMods <\n" +
                "        *\n" +
                "        minecraft\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("thermalfoundation:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
    }

    @Test
    @DisplayName("C7 — Exact recipe preference beats preferredMods")
    public void testCraftingC7_ExactRecipeBeatsPreferredMods() throws IOException {
        File cfg = writeConfig("c7.cfg",
                "policy {\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        thermalfoundation\n" +
                "        *\n" +
                "        minecraft\n" +
                "    >\n" +
                "    S:preferredRecipes <\n" +
                "        enderio:priority_test\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("enderio:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, result.getReason());
        assertEquals(Collections.singletonList("enderio:priority_test"),
                RecipePreferencePolicy.getPolicySnapshot().getConfiguredExactRecipes());
    }

    @Test
    @DisplayName("C8 — Two exact recipes ordered top-to-bottom")
    public void testCraftingC8_TwoExactRecipesOrderedTopToBottom() throws IOException {
        File cfg = writeConfig("c8.cfg",
                "policy {\n" +
                "    S:preferredRecipes <\n" +
                "        thermalfoundation:priority_test\n" +
                "        enderio:priority_test\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("thermalfoundation:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, result.getReason());
    }

    @Test
    @DisplayName("C9 — Unknown preferred mod does not break policy resolution")
    public void testCraftingC9_UnknownPreferredMod() throws IOException {
        File cfg = writeConfig("c9.cfg",
                "policy {\n" +
                "    S:preferredMods <\n" +
                "        nonexistentmod\n" +
                "        mekanism\n" +
                "        *\n" +
                "        minecraft\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("mekanism:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
    }

    @Test
    @DisplayName("C10 — Duplicates, case, and whitespace are normalized in policy snapshot")
    public void testCraftingC10_NormalizationInSnapshot() throws IOException {
        File cfg = writeConfig("c10.cfg",
                "policy {\n" +
                "    S:preferredMods <\n" +
                "         Mekanism\n" +
                "        mekanism\n" +
                "         THERMALFOUNDATION \n" +
                "        *\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<String> snapshot = RecipePreferencePolicy.getPolicySnapshot().getPreferredMods();
        assertEquals(Arrays.asList("mekanism", "thermalfoundation", "*"), snapshot);

        List<RecipeOption> options = createCraftingOptions();
        SelectionContext ctx = createCraftingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);
        assertEquals("mekanism:priority_test", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
    }

    // =========================================================================
    // 3. Smelting Policy Matrix (S1 - S8)
    // =========================================================================

    @Test
    @DisplayName("S1 — Default smelting policy selects first modded output")
    public void testSmeltingS1_DefaultConfig() throws IOException {
        File cfg = writeConfig("s1.cfg",
                "smeltingPolicy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createSmeltingOptions();
        SelectionContext ctx = createSmeltingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("smelt:test:thermal", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.AUTOMATIC_MODDED, result.getReason());
        assertTrue(SmeltingPreferencePolicy.getPolicySnapshot().isPreferModdedOverVanilla());
    }

    @Test
    @DisplayName("S2 — Smelting policy disabled preserves native default")
    public void testSmeltingS2_Disabled() throws IOException {
        File cfg = writeConfig("s2.cfg",
                "smeltingPolicy {\n" +
                "    B:preferModdedOverVanilla=false\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createSmeltingOptions();
        SelectionContext ctx = createSmeltingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertNull(result.getSelectedRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, result.getReason());
        assertFalse(SmeltingPreferencePolicy.getPolicySnapshot().isPreferModdedOverVanilla());
    }

    @Test
    @DisplayName("S3 — Smelting preferredMods selects winning output mod")
    public void testSmeltingS3_PreferredMods() throws IOException {
        File cfg = writeConfig("s3.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        thermalfoundation\n" +
                "        *\n" +
                "        minecraft\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createSmeltingOptions();
        SelectionContext ctx = createSmeltingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("smelt:test:mekanism", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
    }

    @Test
    @DisplayName("S4 — Vanilla explicitly first in smelting preferredMods")
    public void testSmeltingS4_VanillaExplicitlyFirst() throws IOException {
        File cfg = writeConfig("s4.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredMods <\n" +
                "        minecraft\n" +
                "        *\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createSmeltingOptions();
        SelectionContext ctx = createSmeltingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("smelt:test:vanilla", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, result.getReason());
    }

    @Test
    @DisplayName("S5 — preferredOutputs beats preferredMods")
    public void testSmeltingS5_PreferredOutputsBeatsPreferredMods() throws IOException {
        File cfg = writeConfig("s5.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        *\n" +
                "    >\n" +
                "    S:preferredOutputs <\n" +
                "        thermalfoundation:rp_test_material\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = createSmeltingOptions();
        SelectionContext ctx = createSmeltingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("smelt:test:thermal", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, result.getReason());
    }

    @Test
    @DisplayName("S6 — preferredOutputs with metadata selects exact meta")
    public void testSmeltingS6_MetadataSpecificSelector() throws IOException {
        File cfg = writeConfig("s6.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredOutputs <\n" +
                "        thermalfoundation:rp_test_material@7\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("smelt:test:thermal0", new ItemStack(thermalSmeltItem, 1, 0)),
                new RecipeOption("smelt:test:thermal7", new ItemStack(thermalSmeltItem, 1, 7))
        );
        SelectionContext ctx = createSmeltingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        assertEquals("smelt:test:thermal7", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, result.getReason());
    }

    @Test
    @DisplayName("S7 — preferredOutputs without metadata matches any meta")
    public void testSmeltingS7_OutputWithoutMetadataMatchesAnyMeta() throws IOException {
        File cfg = writeConfig("s7.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredOutputs <\n" +
                "        thermalfoundation:rp_test_material\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<RecipeOption> options = Arrays.asList(
                new RecipeOption("smelt:test:thermal0", new ItemStack(thermalSmeltItem, 1, 0)),
                new RecipeOption("smelt:test:thermal7", new ItemStack(thermalSmeltItem, 1, 7))
        );
        SelectionContext ctx = createSmeltingContext(options);

        SelectionServiceResult result = handleQuery(ctx, options);

        // Matches both; chooses first in natural order
        assertEquals("smelt:test:thermal0", result.getSelectedRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, result.getReason());
    }

    @Test
    @DisplayName("S8 — Malformed selectors in preferredOutputs are filtered out in snapshot")
    public void testSmeltingS8_MalformedSelectorsFiltered() throws IOException {
        File cfg = writeConfig("s8.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredOutputs <\n" +
                "        broken selector\n" +
                "        mekanism:rp_test_ingot@-1\n" +
                "        mekanism:rp_test_ingot@999999\n" +
                "        mekanism:rp_test_ingot@abc\n" +
                "        mekanism:rp_test_ingot@3\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg);

        List<String> snapshot = SmeltingPreferencePolicy.getPolicySnapshot().getPreferredOutputs();
        assertEquals(Collections.singletonList("mekanism:rp_test_ingot@3"), snapshot);
    }

    // =========================================================================
    // 4. Full Priority Hierarchy Verification
    // =========================================================================

    @Test
    @DisplayName("4.1 Crafting full priority hierarchy: CURRENT_CONTEXT > PLAYER_PREFERENCE > preferredRecipes > preferredMods > preferModdedOverVanilla > NATIVE_DEFAULT")
    public void testCraftingFullPriorityHierarchy() throws IOException {
        // Step 1: Native Default (preferModdedOverVanilla=false)
        File cfgLevel1 = writeConfig("h_craft_1.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=false\n" +
                "}\n");
        PolymorphConfig.load(cfgLevel1);
        List<RecipeOption> options = createCraftingOptions();
        SelectionServiceResult res1 = handleQuery(createCraftingContext(options), options);
        assertNull(res1.getSelectedRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, res1.getReason());

        // Step 2: preferModdedOverVanilla=true wins over Native Default
        File cfgLevel2 = writeConfig("h_craft_2.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "}\n");
        PolymorphConfig.load(cfgLevel2);
        SelectionServiceResult res2 = handleQuery(createCraftingContext(options), options);
        assertEquals("thermalfoundation:priority_test", res2.getSelectedRecipeKey());
        assertEquals(SelectionReason.AUTOMATIC_MODDED, res2.getReason());

        // Step 3: preferredMods (mekanism) wins over automatic modded (thermalfoundation)
        File cfgLevel3 = writeConfig("h_craft_3.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        *\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfgLevel3);
        SelectionServiceResult res3 = handleQuery(createCraftingContext(options), options);
        assertEquals("mekanism:priority_test", res3.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, res3.getReason());

        // Step 4: preferredRecipes (enderio) wins over preferredMods (mekanism)
        File cfgLevel4 = writeConfig("h_craft_4.cfg",
                "policy {\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        *\n" +
                "    >\n" +
                "    S:preferredRecipes <\n" +
                "        enderio:priority_test\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfgLevel4);
        SelectionServiceResult res4 = handleQuery(createCraftingContext(options), options);
        assertEquals("enderio:priority_test", res4.getSelectedRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, res4.getReason());

        // Step 5: PLAYER_PREFERENCE (minecraft) stored in playerData wins over preferredRecipes (enderio)
        String fingerprint = ConflictFingerprint.create(options);
        assertNotNull(fingerprint);
        PlayerRecipePreferences.remember(this.playerData, fingerprint, "minecraft:priority_test");

        SelectionServiceResult res5 = handleQuery(createCraftingContext(options), options);
        assertEquals("minecraft:priority_test", res5.getSelectedRecipeKey());
        assertEquals(SelectionReason.PLAYER_PREFERENCE, res5.getReason());

        // Step 6: CURRENT_CONTEXT (thermalfoundation) wins over PLAYER_PREFERENCE when non-override
        SelectionContext currentCtx = new MockContext(
                options,
                SelectionPolicyType.RECIPE,
                SelectionPersistencePolicy.PLAYER_PERSISTENT,
                "thermalfoundation:priority_test",
                null);
        SelectionServiceResult res6 = handleQuery(currentCtx, options);
        assertEquals("thermalfoundation:priority_test", res6.getSelectedRecipeKey());
        assertEquals(SelectionReason.CURRENT_CONTEXT, res6.getReason());
    }

    @Test
    @DisplayName("4.2 Smelting full priority hierarchy: CURRENT_CONTEXT > PLAYER_PREFERENCE > preferredOutputs > preferredMods > preferModdedOverVanilla > NATIVE_DEFAULT")
    public void testSmeltingFullPriorityHierarchy() throws IOException {
        List<RecipeOption> options = createSmeltingOptions();

        // Step 1: Native Default
        File cfg1 = writeConfig("h_smelt_1.cfg",
                "smeltingPolicy {\n" +
                "    B:preferModdedOverVanilla=false\n" +
                "}\n");
        PolymorphConfig.load(cfg1);
        SelectionServiceResult res1 = handleQuery(createSmeltingContext(options), options);
        assertNull(res1.getSelectedRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, res1.getReason());

        // Step 2: preferModdedOverVanilla=true wins over Native Default
        File cfg2 = writeConfig("h_smelt_2.cfg",
                "smeltingPolicy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "}\n");
        PolymorphConfig.load(cfg2);
        SelectionServiceResult res2 = handleQuery(createSmeltingContext(options), options);
        assertEquals("smelt:test:thermal", res2.getSelectedRecipeKey());
        assertEquals(SelectionReason.AUTOMATIC_MODDED, res2.getReason());

        // Step 3: preferredMods (mekanism) wins over automatic modded
        File cfg3 = writeConfig("h_smelt_3.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        *\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg3);
        SelectionServiceResult res3 = handleQuery(createSmeltingContext(options), options);
        assertEquals("smelt:test:mekanism", res3.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, res3.getReason());

        // Step 4: preferredOutputs (thermal) wins over preferredMods (mekanism)
        File cfg4 = writeConfig("h_smelt_4.cfg",
                "smeltingPolicy {\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        *\n" +
                "    >\n" +
                "    S:preferredOutputs <\n" +
                "        thermalfoundation:rp_test_material\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfg4);
        SelectionServiceResult res4 = handleQuery(createSmeltingContext(options), options);
        assertEquals("smelt:test:thermal", res4.getSelectedRecipeKey());
        assertEquals(SelectionReason.EXACT_RECIPE_POLICY, res4.getReason());

        // Step 5: PLAYER_PREFERENCE (vanilla) stored in playerData wins over preferredOutputs
        String fingerprint = ConflictFingerprint.create(options);
        assertNotNull(fingerprint);
        PlayerRecipePreferences.remember(this.playerData, fingerprint, "smelt:test:vanilla");

        SelectionServiceResult res5 = handleQuery(createSmeltingContext(options), options);
        assertEquals("smelt:test:vanilla", res5.getSelectedRecipeKey());
        assertEquals(SelectionReason.PLAYER_PREFERENCE, res5.getReason());

        // Step 6: CURRENT_CONTEXT (mekanism) wins over PLAYER_PREFERENCE when non-override
        SelectionContext currentCtx = new MockContext(
                options,
                SelectionPolicyType.SMELTING,
                SelectionPersistencePolicy.PLAYER_PERSISTENT,
                "smelt:test:mekanism",
                null);
        SelectionServiceResult res6 = handleQuery(currentCtx, options);
        assertEquals("smelt:test:mekanism", res6.getSelectedRecipeKey());
        assertEquals(SelectionReason.CURRENT_CONTEXT, res6.getReason());
    }

    // =========================================================================
    // 5. Fresh Generated Config Test
    // =========================================================================

    @Test
    @DisplayName("5. Fresh generated config file contains both policy categories, keys, and comments")
    public void testFreshGeneratedConfig() throws IOException {
        File freshFile = new File(this.tempDir.toFile(), "fresh_generated.cfg");
        assertFalse(freshFile.exists());

        PolymorphConfig.load(freshFile);
        assertTrue(freshFile.exists(), "Configuration file should be generated on load");

        String text = new String(Files.readAllBytes(freshFile.toPath()), StandardCharsets.UTF_8);

        // Verify categories
        assertTrue(text.contains("policy {"), "Must contain policy category");
        assertTrue(text.toLowerCase(java.util.Locale.ROOT).contains("smeltingpolicy {"),
                "Must contain smeltingPolicy category");

        // Verify keys
        assertTrue(text.contains("B:preferModdedOverVanilla="), "Must contain preferModdedOverVanilla toggle");
        assertTrue(text.contains("S:preferredMods"), "Must contain preferredMods setting");
        assertTrue(text.contains("S:preferredRecipes"), "Must contain preferredRecipes setting");
        assertTrue(text.contains("S:preferredOutputs"), "Must contain preferredOutputs setting");

        // Verify comments and examples
        assertTrue(text.contains("Crafting and recipe-backed machine default-selection policy"), "Must contain policy description");
        assertTrue(text.contains("Furnace-only default-selection policy"), "Must contain smeltingPolicy description");
        assertTrue(text.contains("thermalfoundation"), "Must contain example mod");
        assertTrue(text.contains("enderio:example_recipe"), "Must contain example recipe");
        assertTrue(text.contains("mekanism:ingot@0"), "Must contain example output with meta");
    }

    // =========================================================================
    // 6. Reload Across Different Configs in Single JVM
    // =========================================================================

    @Test
    @DisplayName("6. JVM config reload (A -> B -> C) does not leak static state between configurations")
    public void testJvmConfigReloadDoesNotLeakStaticState() throws IOException {
        List<RecipeOption> options = createCraftingOptions();

        // Config A: Mekanism > Thermal > Minecraft
        File cfgA = writeConfig("reload_a.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "    S:preferredMods <\n" +
                "        mekanism\n" +
                "        thermalfoundation\n" +
                "        *\n" +
                "        minecraft\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfgA);
        SelectionServiceResult resA = handleQuery(createCraftingContext(options), options);
        assertEquals("mekanism:priority_test", resA.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, resA.getReason());

        // Config B: Minecraft > *
        File cfgB = writeConfig("reload_b.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=true\n" +
                "    S:preferredMods <\n" +
                "        minecraft\n" +
                "        *\n" +
                "    >\n" +
                "}\n");
        PolymorphConfig.load(cfgB);
        SelectionServiceResult resB = handleQuery(createCraftingContext(options), options);
        assertEquals("minecraft:priority_test", resB.getSelectedRecipeKey());
        assertEquals(SelectionReason.MOD_PRIORITY, resB.getReason());

        // Config C: No preferredMods, preferModdedOverVanilla=false
        File cfgC = writeConfig("reload_c.cfg",
                "policy {\n" +
                "    B:preferModdedOverVanilla=false\n" +
                "}\n");
        PolymorphConfig.load(cfgC);
        SelectionServiceResult resC = handleQuery(createCraftingContext(options), options);
        assertNull(resC.getSelectedRecipeKey());
        assertEquals(SelectionReason.NATIVE_DEFAULT, resC.getReason());
        assertEquals(Collections.emptyList(), RecipePreferencePolicy.getPolicySnapshot().getPreferredMods());
        assertFalse(RecipePreferencePolicy.getPolicySnapshot().isPreferModdedOverVanilla());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private File writeConfig(String filename, String content) throws IOException {
        File cfg = new File(this.tempDir.toFile(), filename);
        Files.write(cfg.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return cfg;
    }

    private SelectionServiceResult handleQuery(SelectionContext ctx, List<RecipeOption> options) {
        List<String> keysBefore = extractKeys(options);
        int sizeBefore = options.size();

        SelectionServiceResult result = SelectionService.handle(
                null, this.playerData, ctx, SelectionCommand.query());

        // Verify options list was not mutated or reordered
        assertEquals(sizeBefore, options.size(), "Options list size must remain unchanged");
        assertEquals(keysBefore, extractKeys(options), "Options list order must not be mutated");

        return result;
    }

    private static List<String> extractKeys(List<RecipeOption> options) {
        ArrayList<String> keys = new ArrayList<String>(options.size());
        for (RecipeOption opt : options) {
            keys.add(opt == null ? null : opt.getRecipeKey());
        }
        return keys;
    }

    private List<RecipeOption> createCraftingOptions() {
        return Arrays.asList(
                new RecipeOption("minecraft:priority_test", new ItemStack(Items.IRON_INGOT)),
                new RecipeOption("thermalfoundation:priority_test", new ItemStack(Items.GOLD_INGOT)),
                new RecipeOption("mekanism:priority_test", new ItemStack(Items.DIAMOND)),
                new RecipeOption("enderio:priority_test", new ItemStack(Items.EMERALD))
        );
    }

    private List<RecipeOption> createSmeltingOptions() {
        return Arrays.asList(
                new RecipeOption("smelt:test:vanilla", new ItemStack(vanillaSmeltItem, 1, 0)),
                new RecipeOption("smelt:test:thermal", new ItemStack(thermalSmeltItem, 1, 0)),
                new RecipeOption("smelt:test:mekanism", new ItemStack(mekanismSmeltItem, 1, 0))
        );
    }

    private SelectionContext createCraftingContext(List<RecipeOption> options) {
        return new MockContext(options, SelectionPolicyType.RECIPE);
    }

    private SelectionContext createCraftingContext() {
        return createCraftingContext(createCraftingOptions());
    }

    private SelectionContext createSmeltingContext(List<RecipeOption> options) {
        return new MockContext(options, SelectionPolicyType.SMELTING);
    }

    private static final class MockContext implements SelectionContext {
        private final List<RecipeOption> options;
        private final SelectionPolicyType policyType;
        private final SelectionPersistencePolicy persistencePolicy;
        @Nullable private final Container container;
        private final ItemStack input = new ItemStack(Items.IRON_INGOT, 1, 0);
        @Nullable private String selectedKey;

        MockContext(List<RecipeOption> options, SelectionPolicyType policyType) {
            this(options, policyType, SelectionPersistencePolicy.PLAYER_PERSISTENT, null, null);
        }

        MockContext(
                List<RecipeOption> options,
                SelectionPolicyType policyType,
                SelectionPersistencePolicy persistencePolicy,
                @Nullable String initialSelection,
                @Nullable Container container) {
            this.options = options;
            this.policyType = policyType;
            this.persistencePolicy = persistencePolicy;
            this.selectedKey = initialSelection;
            this.container = container;
        }

        @Override public SelectionPolicyType getPolicyType() { return this.policyType; }
        @Override public SelectionPersistencePolicy getPersistencePolicy() { return this.persistencePolicy; }
        @Override public Container getContainer() { return this.container; }
        @Override public Slot getResultSlot() { return null; }
        @Override public int getInputCount() { return 1; }
        @Override public ItemStack getInputStack(int index) { return index == 0 ? this.input : ItemStack.EMPTY; }
        @Override public List<RecipeOption> findOptions(World world) { return this.options; }
        @Override public boolean select(String recipeKey, World world) {
            this.selectedKey = recipeKey;
            return true;
        }
        @Override public void clearSelection() { this.selectedKey = null; }
        @Nullable @Override public String getSelectedRecipeKey() { return this.selectedKey; }
    }
}
