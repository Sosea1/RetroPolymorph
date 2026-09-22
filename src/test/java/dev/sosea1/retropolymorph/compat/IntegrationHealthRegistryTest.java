package dev.sosea1.retropolymorph.compat;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationHealthRegistryTest {

    @BeforeEach
    @AfterEach
    public void cleanup() {
        IntegrationHealthRegistry.resetForTests();
    }

    @Test
    public void testInitialStateIsUnseenWhenEnabled() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "test_mod", "Test Mod", 100);
        IntegrationHealthRegistry.register(descriptor, true);

        IntegrationHealthRegistry.HealthSnapshot snapshot = IntegrationHealthRegistry.getHealth("test_mod");
        assertNotNull(snapshot);
        assertEquals(IntegrationHealthRegistry.Status.UNSEEN, snapshot.getStatus());
        assertTrue(snapshot.isEnabled());
        assertEquals(0, snapshot.getDetections());
        assertEquals(0, snapshot.getBindingFailures());
        assertEquals(0, snapshot.getProbeFailures());
    }

    @Test
    public void testDisabledIntegrationStatus() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "disabled_mod", "Disabled Mod", 100);
        IntegrationHealthRegistry.register(descriptor, false);

        IntegrationHealthRegistry.HealthSnapshot snapshot = IntegrationHealthRegistry.getHealth("disabled_mod");
        assertNotNull(snapshot);
        assertEquals(IntegrationHealthRegistry.Status.DISABLED, snapshot.getStatus());
    }

    @Test
    public void testSuccessfulDetectionTransitionsToOk() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "ok_mod", "OK Mod", 100);
        IntegrationHealthRegistry.register(descriptor, true);

        IntegrationHealthRegistry.recordDetection("ok_mod");
        IntegrationHealthRegistry.recordDetection("ok_mod");

        IntegrationHealthRegistry.HealthSnapshot snapshot = IntegrationHealthRegistry.getHealth("ok_mod");
        assertNotNull(snapshot);
        assertEquals(IntegrationHealthRegistry.Status.OK, snapshot.getStatus());
        assertEquals(2, snapshot.getDetections());
    }

    @Test
    public void testBlockedFallbackTransitionsToOk() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "guard_mod", "Guard Mod", 100);
        IntegrationHealthRegistry.register(descriptor, true);

        IntegrationHealthRegistry.recordBlockedFallback("guard_mod");

        IntegrationHealthRegistry.HealthSnapshot snapshot = IntegrationHealthRegistry.getHealth("guard_mod");
        assertNotNull(snapshot);
        assertEquals(IntegrationHealthRegistry.Status.OK, snapshot.getStatus());
        assertEquals(1, snapshot.getBlockedFallbacks());
    }

    @Test
    public void testBindingFailureTransitionsToDegraded() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "failing_mod", "Failing Mod", 100);
        IntegrationHealthRegistry.register(descriptor, true);

        IntegrationHealthRegistry.recordDetection("failing_mod");
        assertEquals(IntegrationHealthRegistry.Status.OK, IntegrationHealthRegistry.getHealth("failing_mod").getStatus());

        IntegrationHealthRegistry.recordBindingFailure(
                "failing_mod", "resolveMethod", new ClassNotFoundException("com.example.Missing"));

        IntegrationHealthRegistry.HealthSnapshot snapshot = IntegrationHealthRegistry.getHealth("failing_mod");
        assertNotNull(snapshot);
        assertEquals(IntegrationHealthRegistry.Status.DEGRADED, snapshot.getStatus());
        assertEquals(1, snapshot.getBindingFailures());
        assertEquals("resolveMethod", snapshot.getLastFailureOperation());
        assertEquals("java.lang.ClassNotFoundException", snapshot.getLastFailureClass());
        assertEquals("com.example.Missing", snapshot.getLastFailureMessage());
        assertTrue(snapshot.getLastFailureTimestamp() > 0);
    }

    @Test
    public void testProbeFailureTransitionsToDegraded() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "probe_mod", "Probe Mod", 100);
        IntegrationHealthRegistry.register(descriptor, true);

        IntegrationHealthRegistry.recordProbeFailure(
                "probe_mod", "probe", new NoSuchMethodError("bad signature"));

        IntegrationHealthRegistry.HealthSnapshot snapshot = IntegrationHealthRegistry.getHealth("probe_mod");
        assertNotNull(snapshot);
        assertEquals(IntegrationHealthRegistry.Status.DEGRADED, snapshot.getStatus());
        assertEquals(1, snapshot.getProbeFailures());
        assertEquals("probe", snapshot.getLastFailureOperation());
    }

    @Test
    public void testAdapterAssociationAndLookup() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "tconstruct", "Tinkers' Construct", 100);
        IntegrationHealthRegistry.register(descriptor, true);

        ResourceLocation adapterId = new ResourceLocation("retropolymorph", "tconstruct_crafting_station");
        IntegrationHealthRegistry.associateAdapter(adapterId, "tconstruct");

        IntegrationHealthRegistry.recordAdapterMatch(adapterId);
        IntegrationHealthRegistry.HealthSnapshot snapshot = IntegrationHealthRegistry.getHealthByAdapter(adapterId);
        assertNotNull(snapshot);
        assertEquals("tconstruct", snapshot.getId());
        assertEquals(1, snapshot.getDetections());
        assertEquals(IntegrationHealthRegistry.Status.OK, snapshot.getStatus());
    }

    @Test
    public void testGetAllSnapshots() {
        IntegrationHealthRegistry.register(new IntegrationDescriptor("mod1", "Mod 1", 100), true);
        IntegrationHealthRegistry.register(new IntegrationDescriptor("mod2", "Mod 2", 100), false);

        List<IntegrationHealthRegistry.HealthSnapshot> list = IntegrationHealthRegistry.getAllSnapshots();
        assertEquals(2, list.size());
    }

    @Test
    public void testExactAdapterAssociationWithoutHeuristicFallback() {
        IntegrationDescriptor descriptor = new IntegrationDescriptor(
                "mekanism", "Mekanism", 100);
        IntegrationHealthRegistry.register(descriptor, true);

        // Even if adapterId starts with "mekanism_", without exact association it must return null
        ResourceLocation unassociatedAdapterId = new ResourceLocation("retropolymorph", "mekanism_formulaic_assemblicator");
        assertNull(IntegrationHealthRegistry.getHealthByAdapter(unassociatedAdapterId));

        // Now associate explicitly
        IntegrationHealthRegistry.associateAdapter("mekanism", unassociatedAdapterId);
        assertNotNull(IntegrationHealthRegistry.getHealthByAdapter(unassociatedAdapterId));
        assertEquals("mekanism", IntegrationHealthRegistry.getHealthByAdapter(unassociatedAdapterId).getId());
    }
}
