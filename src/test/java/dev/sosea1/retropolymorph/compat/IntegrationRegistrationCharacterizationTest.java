package dev.sosea1.retropolymorph.compat;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapterInfo;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapters;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdaptersTestAccess;
import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProvidersTestAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationRegistrationCharacterizationTest {

    @BeforeEach
    @AfterEach
    void reset() {
        RecipeSelectionAdaptersTestAccess.reset();
        CompatibilityBootstrap.resetForTests();
        ExternalCraftingSelectionProvidersTestAccess.reset();
    }

    @Test
    void testBuiltinAdapterRegistrationOrderAndPriorities() {
        CompatibilityBootstrap.init();

        List<RecipeSelectionAdapterInfo> adapters = RecipeSelectionAdapters.getRegisteredAdapters();
        assertFalse(adapters.isEmpty(), "Adapters should not be empty after init");

        List<String> entries = new ArrayList<String>();
        for (RecipeSelectionAdapterInfo info : adapters) {
            entries.add(info.getId().toString() + "@" + info.getPriority());
        }

        // Snapshot of the baseline registration order and priority:
        // Priority 1200: Immersive Engineering guard
        // Priority 1190: Engineer's Decor guard
        // Priority 1100: Retro Sophisticated Backpacks
        // Priority 1090: Extra Utilities 2
        // Priority 1000: EnderIO, Thaumcraft
        // Priority 950: Mekanism
        // Priority 940: Thermal
        // Priority 900: Refined Storage (crafting, pattern)
        // Priority 800: AE2 (pattern, crafting), Extended Crafting (table, ender crafter),
        //               IC2 (industrial wb, batch crafter), Cyclic (wb, crafter),
        //               RFTools (crafter, wb), Tinkers (crafting station), Forestry guard
        List<String> expected = new ArrayList<String>();
        expected.add("retropolymorph:immersive_engineering_mod_workbench_native_selector@1200");
        expected.add("retropolymorph:engineers_decor_native_selector@1190");
        expected.add("retropolymorph:retro_sophisticated_backpacks_crafting_upgrade@1100");
        expected.add("retropolymorph:extrautils2_crafters@1090");
        expected.add("retropolymorph:enderio_crafter@1000");
        expected.add("retropolymorph:thaumcraft_arcane_workbench@1000");
        expected.add("retropolymorph:mekanism_formulaic_assemblicator@950");
        expected.add("retropolymorph:thermal_sequential_fabricator@940");
        expected.add("retropolymorph:refinedstorage_crafting_grid@900");
        expected.add("retropolymorph:refinedstorage_pattern_grid@900");
        expected.add("retropolymorph:ae2_pattern_terminal@800");
        expected.add("retropolymorph:ae2_crafting_terminal@800");
        expected.add("retropolymorph:extended_crafting_table@800");
        expected.add("retropolymorph:extended_crafting_ender_crafter@800");
        expected.add("retropolymorph:ic2_industrial_workbench@800");
        expected.add("retropolymorph:ic2_batch_crafter@800");
        expected.add("retropolymorph:cyclic_workbench@800");
        expected.add("retropolymorph:cyclic_crafter@800");
        expected.add("retropolymorph:rftools_crafter@800");
        expected.add("retropolymorph:rftools_workbench@800");
        expected.add("retropolymorph:tconstruct_crafting_station@800");
        expected.add("retropolymorph:forestry_worktable_guard@800");

        assertEquals(expected, entries);
    }

    @Test
    public void testSemanticInvariants() {
        CompatibilityBootstrap.init();

        List<RecipeSelectionAdapterInfo> adapters = RecipeSelectionAdapters.getRegisteredAdapters();
        java.util.Set<net.minecraft.util.ResourceLocation> seenIds = new java.util.HashSet<net.minecraft.util.ResourceLocation>();

        int previousPriority = Integer.MAX_VALUE;
        for (RecipeSelectionAdapterInfo info : adapters) {
            // Invariant 1: All adapter IDs are unique
            assertTrue(seenIds.add(info.getId()), "Duplicate adapter ID: " + info.getId());

            // Invariant 2: Adapters are strictly ordered by descending priority
            assertTrue(info.getPriority() <= previousPriority,
                    "Adapters must be ordered by descending priority: " + info.getPriority() + " > " + previousPriority);
            previousPriority = info.getPriority();
        }

        // Invariant 3: Guard tier is strictly higher than specialized machine tier
        assertTrue(CompatibilityBootstrap.PRIORITY_NATIVE_RESOLVER_GUARD > CompatibilityBootstrap.PRIORITY_SPECIALIZED_CRAFTER);

        // Invariant 4: Specialized machine tier is strictly higher than standard bench tier
        assertTrue(CompatibilityBootstrap.PRIORITY_SPECIALIZED_CRAFTER > CompatibilityBootstrap.PRIORITY_STANDARD_BENCH);

        // Invariant 5: Standard bench tier is strictly higher than third-party normal addon tier
        assertTrue(CompatibilityBootstrap.PRIORITY_STANDARD_BENCH > dev.sosea1.retropolymorph.api.RetroPolymorphAPI.PRIORITY_NORMAL);

        // Invariant 6: Normal addon tier is strictly higher than fallback tier
        assertTrue(dev.sosea1.retropolymorph.api.RetroPolymorphAPI.PRIORITY_NORMAL > dev.sosea1.retropolymorph.api.RetroPolymorphAPI.PRIORITY_FALLBACK);

        // Invariant 7: Intentional override tier is higher than all built-in tiers
        assertTrue(dev.sosea1.retropolymorph.api.RetroPolymorphAPI.PRIORITY_OVERRIDE > CompatibilityBootstrap.PRIORITY_NATIVE_RESOLVER_GUARD);
    }
}
