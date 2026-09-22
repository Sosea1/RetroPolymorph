package dev.sosea1.retropolymorph.compat;

import dev.sosea1.retropolymorph.api.RecipeSelectionAdapters;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight, thread-safe central registry tracking health, status, and failure counters
 * for all built-in and third-party integrations.
 */
public final class IntegrationHealthRegistry {

    public enum Status {
        OK,
        DEGRADED,
        DISABLED,
        UNSEEN
    }

    public static final class HealthSnapshot {
        private final String id;
        private final String displayName;
        private final boolean enabled;
        private final Status status;
        private final long detections;
        private final long blockedFallbacks;
        private final long bindingFailures;
        private final long probeFailures;
        @Nullable private final String lastFailureOperation;
        @Nullable private final String lastFailureClass;
        @Nullable private final String lastFailureMessage;
        private final long lastFailureTimestamp;

        public HealthSnapshot(
                String id,
                String displayName,
                boolean enabled,
                Status status,
                long detections,
                long blockedFallbacks,
                long bindingFailures,
                long probeFailures,
                @Nullable String lastFailureOperation,
                @Nullable String lastFailureClass,
                @Nullable String lastFailureMessage,
                long lastFailureTimestamp) {
            this.id = id;
            this.displayName = displayName;
            this.enabled = enabled;
            this.status = status;
            this.detections = detections;
            this.blockedFallbacks = blockedFallbacks;
            this.bindingFailures = bindingFailures;
            this.probeFailures = probeFailures;
            this.lastFailureOperation = lastFailureOperation;
            this.lastFailureClass = lastFailureClass;
            this.lastFailureMessage = lastFailureMessage;
            this.lastFailureTimestamp = lastFailureTimestamp;
        }

        public String getId() {
            return this.id;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public Status getStatus() {
            return this.status;
        }

        public long getDetections() {
            return this.detections;
        }

        public long getBlockedFallbacks() {
            return this.blockedFallbacks;
        }

        public long getBindingFailures() {
            return this.bindingFailures;
        }

        public long getProbeFailures() {
            return this.probeFailures;
        }

        @Nullable
        public String getLastFailureOperation() {
            return this.lastFailureOperation;
        }

        @Nullable
        public String getLastFailureClass() {
            return this.lastFailureClass;
        }

        @Nullable
        public String getLastFailureMessage() {
            return this.lastFailureMessage;
        }

        public long getLastFailureTimestamp() {
            return this.lastFailureTimestamp;
        }

        @Override
        public String toString() {
            return "HealthSnapshot{" +
                    "id='" + this.id + '\'' +
                    ", displayName='" + this.displayName + '\'' +
                    ", status=" + this.status +
                    ", detections=" + this.detections +
                    ", blockedFallbacks=" + this.blockedFallbacks +
                    ", bindingFailures=" + this.bindingFailures +
                    ", probeFailures=" + this.probeFailures +
                    '}';
        }
    }

    private static final class Entry {
        final IntegrationDescriptor descriptor;
        final boolean enabled;
        final AtomicLong detections = new AtomicLong();
        final AtomicLong blockedFallbacks = new AtomicLong();
        final AtomicLong bindingFailures = new AtomicLong();
        final AtomicLong probeFailures = new AtomicLong();

        volatile String lastFailureOperation;
        volatile String lastFailureClass;
        volatile String lastFailureMessage;
        volatile long lastFailureTimestamp;

        Entry(IntegrationDescriptor descriptor, boolean enabled) {
            this.descriptor = descriptor;
            this.enabled = enabled;
        }

        Status computeStatus() {
            if (!this.enabled) {
                return Status.DISABLED;
            }
            if (this.bindingFailures.get() > 0 || this.probeFailures.get() > 0) {
                return Status.DEGRADED;
            }
            if (this.detections.get() > 0 || this.blockedFallbacks.get() > 0) {
                return Status.OK;
            }
            return Status.UNSEEN;
        }

        HealthSnapshot toSnapshot() {
            return new HealthSnapshot(
                    this.descriptor.getId(),
                    this.descriptor.getDisplayName(),
                    this.enabled,
                    computeStatus(),
                    this.detections.get(),
                    this.blockedFallbacks.get(),
                    this.bindingFailures.get(),
                    this.probeFailures.get(),
                    this.lastFailureOperation,
                    this.lastFailureClass,
                    this.lastFailureMessage,
                    this.lastFailureTimestamp);
        }
    }

    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<String, Entry>();
    private static final Map<ResourceLocation, String> ADAPTER_TO_INTEGRATION =
            new ConcurrentHashMap<ResourceLocation, String>();

    private IntegrationHealthRegistry() {
    }

    public static void register(IntegrationDescriptor descriptor, boolean enabled) {
        if (descriptor == null) {
            return;
        }
        ENTRIES.put(descriptor.getId(), new Entry(descriptor, enabled));
    }

    public static void associateAdapter(ResourceLocation adapterId, String integrationId) {
        if (adapterId != null && integrationId != null) {
            ADAPTER_TO_INTEGRATION.put(adapterId, integrationId);
        }
    }

    public static void associateAdapter(String integrationId, ResourceLocation adapterId) {
        associateAdapter(adapterId, integrationId);
    }

    @Nullable
    public static String getIntegrationForAdapter(ResourceLocation adapterId) {
        if (adapterId == null) {
            return null;
        }
        return ADAPTER_TO_INTEGRATION.get(adapterId);
    }

    public static void recordAdapterMatch(ResourceLocation adapterId) {
        String integrationId = getIntegrationForAdapter(adapterId);
        if (integrationId != null) {
            recordDetection(integrationId);
        }
    }

    public static void recordAdapterBlockFallback(ResourceLocation adapterId) {
        String integrationId = getIntegrationForAdapter(adapterId);
        if (integrationId != null) {
            recordBlockedFallback(integrationId);
        }
    }

    public static void recordAdapterFailure(ResourceLocation adapterId, String operation, @Nullable Throwable error) {
        String integrationId = getIntegrationForAdapter(adapterId);
        if (integrationId != null) {
            recordProbeFailure(integrationId, operation, error);
        }
    }

    public static void recordDetection(String integrationId) {
        Entry entry = ENTRIES.get(integrationId);
        if (entry != null) {
            entry.detections.incrementAndGet();
        }
    }

    public static void recordBlockedFallback(String integrationId) {
        Entry entry = ENTRIES.get(integrationId);
        if (entry != null) {
            entry.blockedFallbacks.incrementAndGet();
        }
    }

    public static void recordBindingFailure(String integrationId, String operation, @Nullable Throwable error) {
        Entry entry = ENTRIES.get(integrationId);
        if (entry != null) {
            entry.bindingFailures.incrementAndGet();
            recordFailureDetail(entry, operation, error);
        }
    }

    public static void recordProbeFailure(String integrationId, String operation, @Nullable Throwable error) {
        Entry entry = ENTRIES.get(integrationId);
        if (entry != null) {
            entry.probeFailures.incrementAndGet();
            recordFailureDetail(entry, operation, error);
        }
    }

    private static void recordFailureDetail(Entry entry, String operation, @Nullable Throwable error) {
        entry.lastFailureOperation = operation;
        entry.lastFailureTimestamp = System.currentTimeMillis();
        if (error != null) {
            entry.lastFailureClass = error.getClass().getName();
            entry.lastFailureMessage = error.getMessage();
        }
    }

    @Nullable
    public static HealthSnapshot getHealth(String integrationId) {
        Entry entry = ENTRIES.get(integrationId);
        return entry == null ? null : entry.toSnapshot();
    }

    @Nullable
    public static HealthSnapshot getHealthByAdapter(ResourceLocation adapterId) {
        String integrationId = getIntegrationForAdapter(adapterId);
        return integrationId == null ? null : getHealth(integrationId);
    }

    public static List<HealthSnapshot> getAllSnapshots() {
        List<HealthSnapshot> list = new ArrayList<HealthSnapshot>(ENTRIES.size());
        for (Entry entry : ENTRIES.values()) {
            list.add(entry.toSnapshot());
        }
        return Collections.unmodifiableList(list);
    }

    private static final RecipeSelectionAdapters.ProbeListener PROBE_LISTENER =
            new RecipeSelectionAdapters.ProbeListener() {
                @Override
                public void onMatch(ResourceLocation adapterId, SelectionContext context) {
                    recordAdapterMatch(adapterId);
                }

                @Override
                public void onBlockFallback(ResourceLocation adapterId) {
                    recordAdapterBlockFallback(adapterId);
                }

                @Override
                public void onFailure(ResourceLocation adapterId, Throwable error) {
                    recordAdapterFailure(adapterId, "probe", error);
                }
            };

    public static RecipeSelectionAdapters.ProbeListener getProbeListener() {
        return PROBE_LISTENER;
    }

    static synchronized void resetForTests() {
        ENTRIES.clear();
        ADAPTER_TO_INTEGRATION.clear();
    }
}
