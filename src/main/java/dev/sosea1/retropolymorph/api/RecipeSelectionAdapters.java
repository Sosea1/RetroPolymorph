package dev.sosea1.retropolymorph.api;

import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Copy-on-write adapter registry. Registration is expected during startup;
 * detection is a hot path, so readers iterate one immutable array without
 * allocations or locks.
 */
public final class RecipeSelectionAdapters {

    private static final Entry[] EMPTY = new Entry[0];
    private static volatile Entry[] entries = EMPTY;
    private static long nextOrder;

    private RecipeSelectionAdapters() {
    }

    public static synchronized void register(
            ResourceLocation id,
            int priority,
            RecipeSelectionAdapter adapter) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        if (adapter == null) {
            throw new NullPointerException("adapter");
        }

        Entry[] current = entries;
        for (Entry entry : current) {
            if (entry.info.getId().equals(id)) {
                throw new IllegalArgumentException("Recipe-selection adapter already registered: " + id);
            }
        }

        Entry[] updated = Arrays.copyOf(current, current.length + 1);
        updated[current.length] = new Entry(
                new RecipeSelectionAdapterInfo(id, priority, adapter),
                nextOrder++);
        Arrays.sort(updated, ENTRY_ORDER);
        entries = updated;
    }

    public interface ProbeListener {
        void onMatch(ResourceLocation adapterId, SelectionContext context);
        void onBlockFallback(ResourceLocation adapterId);
        void onFailure(ResourceLocation adapterId, Throwable error);
    }

    private static volatile ProbeListener probeListener;

    public static void setProbeListener(@Nullable ProbeListener listener) {
        probeListener = listener;
    }

    public static AdapterDetectionResult probe(Container container) {
        Entry[] snapshot = entries;
        boolean fallbackBlocked = false;
        ProbeListener listener = probeListener;
        for (Entry entry : snapshot) {
            AdapterDetectionResult result;
            try {
                result = entry.info.getAdapter().probe(container);
            } catch (RuntimeException | LinkageError error) {
                if (listener != null) {
                    listener.onFailure(entry.info.getId(), error);
                }
                continue;
            }
            if (result.isMatch()) {
                if (listener != null) {
                    listener.onMatch(entry.info.getId(), result.getContext());
                }
                return result;
            }
            if (result.isBlockFallback()) {
                if (listener != null) {
                    listener.onBlockFallback(entry.info.getId());
                }
                fallbackBlocked = true;
            }
        }
        return fallbackBlocked ? AdapterDetectionResult.blockFallback() : AdapterDetectionResult.miss();
    }

    @Nullable
    public static SelectionContext detect(Container container) {
        AdapterDetectionResult result = probe(container);
        return result.isMatch() ? result.getContext() : null;
    }

    public static boolean blocksFallbackDetection(Container container) {
        AdapterDetectionResult result = probe(container);
        return result.isBlockFallback();
    }

    public static List<RecipeSelectionAdapterInfo> getRegisteredAdapters() {
        Entry[] snapshot = entries;
        ArrayList<RecipeSelectionAdapterInfo> result =
                new ArrayList<RecipeSelectionAdapterInfo>(snapshot.length);
        for (Entry entry : snapshot) {
            result.add(entry.info);
        }
        return Collections.unmodifiableList(result);
    }

    static synchronized void resetForTests() {
        entries = EMPTY;
        nextOrder = 0;
        probeListener = null;
    }

    private static final Comparator<Entry> ENTRY_ORDER = new Comparator<Entry>() {
        @Override
        public int compare(Entry left, Entry right) {
            int byPriority = Integer.compare(right.info.getPriority(), left.info.getPriority());
            if (byPriority != 0) {
                return byPriority;
            }
            return Long.compare(left.order, right.order);
        }
    };

    private static final class Entry {
        private final RecipeSelectionAdapterInfo info;
        private final long order;

        private Entry(RecipeSelectionAdapterInfo info, long order) {
            this.info = info;
            this.order = order;
        }
    }
}
