package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.compat.fastsuite.FastSuiteInterop;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class RecipeResolver {

    private static final int CACHE_SIZE = 16;

    /* Integrated client/server can touch recipes on different threads. */
    private static final ThreadLocal<CraftingMatchCache> CACHE =
            new ThreadLocal<CraftingMatchCache>() {
                @Override
                protected CraftingMatchCache initialValue() {
                    return new CraftingMatchCache(CACHE_SIZE);
                }
            };

    private static final AtomicLong CACHE_HITS = new AtomicLong();
    private static final AtomicLong CACHE_MISSES = new AtomicLong();
    private static final AtomicLong FULL_SCANS = new AtomicLong();
    private static final AtomicLong FASTSUITE_SCANS = new AtomicLong();
    private static final AtomicLong RECIPES_VISITED = new AtomicLong();
    private static final AtomicLong VISITED_COUNTED_SCANS = new AtomicLong();
    private static final AtomicLong SCAN_NANOS = new AtomicLong();

    private RecipeResolver() {
    }

    public static List<IRecipe> findAllMatches(InventoryCrafting matrix, World world) {
        if (matrix == null || world == null || isEmpty(matrix)) {
            return Collections.emptyList();
        }

        InventoryCrafting clean = RecipeProbe.sanitizeMatrix(matrix);
        CraftingMatchCache cache = CACHE.get();
        List<IRecipe> cached = cache.get(clean, world);
        if (cached != null) {
            CACHE_HITS.incrementAndGet();
            return cached;
        }

        CACHE_MISSES.incrementAndGet();
        long started = System.nanoTime();

        // Exactly two lookup modes:
        // 1) current FastSuite ordered all-match visitor when FastSuite is installed;
        // 2) complete Forge registry scan when FastSuite is absent or its current API failed.
        List<IRecipe> fastSuiteMatches = FastSuiteInterop.findAllMatches(clean, world);
        if (fastSuiteMatches != null) {
            FASTSUITE_SCANS.incrementAndGet();
            SCAN_NANOS.addAndGet(System.nanoTime() - started);
            cache.put(clean, world, fastSuiteMatches);
            return fastSuiteMatches;
        }

        FULL_SCANS.incrementAndGet();
        FastSuiteInterop.noteForgeScan();
        List<IRecipe> matches = new ArrayList<IRecipe>(4);
        long visited = 0L;
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            visited++;
            if (recipe != null && RecipeProbe.matches(recipe, clean, world)) {
                matches.add(recipe);
            }
        }

        RECIPES_VISITED.addAndGet(visited);
        VISITED_COUNTED_SCANS.incrementAndGet();
        SCAN_NANOS.addAndGet(System.nanoTime() - started);

        List<IRecipe> result = matches.isEmpty()
                ? Collections.<IRecipe>emptyList()
                : Collections.unmodifiableList(matches);
        cache.put(clean, world, result);
        return result;
    }

    public static Stats getStats() {
        return new Stats(
                CACHE_HITS.get(),
                CACHE_MISSES.get(),
                FULL_SCANS.get(),
                FASTSUITE_SCANS.get(),
                RECIPES_VISITED.get(),
                VISITED_COUNTED_SCANS.get(),
                SCAN_NANOS.get());
    }

    public static void resetStats() {
        CACHE_HITS.set(0L);
        CACHE_MISSES.set(0L);
        FULL_SCANS.set(0L);
        FASTSUITE_SCANS.set(0L);
        RECIPES_VISITED.set(0L);
        VISITED_COUNTED_SCANS.set(0L);
        SCAN_NANOS.set(0L);
    }

    private static boolean isEmpty(InventoryCrafting matrix) {
        for (int slot = 0; slot < matrix.getSizeInventory(); slot++) {
            if (!matrix.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static final class Stats {
        public final long cacheHits;
        public final long cacheMisses;
        public final long fullScans;
        public final long fastSuiteScans;
        public final long recipesVisited;
        public final long visitedCountedScans;
        public final long scanNanos;

        private Stats(
                long cacheHits,
                long cacheMisses,
                long fullScans,
                long fastSuiteScans,
                long recipesVisited,
                long visitedCountedScans,
                long scanNanos) {
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.fullScans = fullScans;
            this.fastSuiteScans = fastSuiteScans;
            this.recipesVisited = recipesVisited;
            this.visitedCountedScans = visitedCountedScans;
            this.scanNanos = scanNanos;
        }

        public long scans() {
            return this.fullScans + this.fastSuiteScans;
        }

        public double averageScanMicros() {
            long scans = scans();
            return scans == 0L
                    ? 0.0D
                    : (this.scanNanos / 1000.0D) / scans;
        }

        public double averageRecipesVisited() {
            return this.visitedCountedScans == 0L
                    ? 0.0D
                    : (double) this.recipesVisited / (double) this.visitedCountedScans;
        }
    }
}
