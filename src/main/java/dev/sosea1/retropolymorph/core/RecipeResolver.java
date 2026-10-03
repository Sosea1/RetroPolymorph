package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.compat.fastsuite.FastSuiteInterop;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

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

    private static final LongAdder CACHE_HITS = new LongAdder();
    private static final LongAdder CACHE_MISSES = new LongAdder();
    private static final LongAdder FULL_SCANS = new LongAdder();
    private static final LongAdder FASTSUITE_SCANS = new LongAdder();
    private static final LongAdder RECIPES_VISITED = new LongAdder();
    private static final LongAdder VISITED_COUNTED_SCANS = new LongAdder();
    private static final LongAdder SCAN_NANOS = new LongAdder();

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
            CACHE_HITS.increment();
            return cached;
        }

        CACHE_MISSES.increment();
        long started = System.nanoTime();

        List<IRecipe> fastSuiteMatches = FastSuiteInterop.findAllMatches(clean, world);
        if (fastSuiteMatches != null) {
            FASTSUITE_SCANS.increment();
            SCAN_NANOS.add(System.nanoTime() - started);
            cache.put(clean, world, fastSuiteMatches);
            return fastSuiteMatches;
        }

        FULL_SCANS.increment();
        FastSuiteInterop.noteForgeScan();
        List<IRecipe> matches = new ArrayList<IRecipe>(4);
        long visited = 0L;
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            visited++;
            if (recipe != null && RecipeProbe.matches(recipe, clean, world)) {
                matches.add(recipe);
            }
        }

        RECIPES_VISITED.add(visited);
        VISITED_COUNTED_SCANS.increment();
        SCAN_NANOS.add(System.nanoTime() - started);

        List<IRecipe> result = matches.isEmpty()
                ? Collections.<IRecipe>emptyList()
                : Collections.unmodifiableList(matches);
        cache.put(clean, world, result);
        return result;
    }

    public static Stats getStats() {
        return new Stats(
                CACHE_HITS.sum(),
                CACHE_MISSES.sum(),
                FULL_SCANS.sum(),
                FASTSUITE_SCANS.sum(),
                RECIPES_VISITED.sum(),
                VISITED_COUNTED_SCANS.sum(),
                SCAN_NANOS.sum());
    }

    public static void resetStats() {
        CACHE_HITS.reset();
        CACHE_MISSES.reset();
        FULL_SCANS.reset();
        FASTSUITE_SCANS.reset();
        RECIPES_VISITED.reset();
        VISITED_COUNTED_SCANS.reset();
        SCAN_NANOS.reset();
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
