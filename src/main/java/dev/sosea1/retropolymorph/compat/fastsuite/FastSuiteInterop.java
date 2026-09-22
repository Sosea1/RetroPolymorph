package dev.sosea1.retropolymorph.compat.fastsuite;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optional bridge to Retro FastSuite's conservative candidate API.
 *
 * <p>FastSuite only narrows the ordered candidate set here. RetroPolymorph still
 * executes {@link IRecipe#matches} through {@link RecipeProbe}, so one broken
 * third-party recipe cannot disable the accelerator and force a second full
 * registry scan.</p>
 */
public final class FastSuiteInterop {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    private static final String[] API_CLASSES = {
            "com.sosea1.fastsuite112.api.FastSuiteAPI",
            "dev.sosea1.fastsuite112.api.FastSuiteAPI"
    };

    private static final AtomicLong FAILURES = new AtomicLong();
    private static final ConcurrentMap<String, Boolean> REPORTED_FAILURES =
            new ConcurrentHashMap<String, Boolean>();

    private static volatile boolean initialized;
    private static volatile Method getCandidateRecipes;
    private static volatile String resolvedApiClass;
    private static volatile Mode lastMode = Mode.FORGE_REGISTRY;

    private FastSuiteInterop() {
    }

    public enum Mode {
        FASTSUITE_CANDIDATES,
        FORGE_REGISTRY
    }

    public static boolean isInstalled() {
        return ensureCurrentApi();
    }

    /**
     * Returns every matching recipe in exact FastSuite/Forge registry order.
     *
     * @return matching recipes, or {@code null} when FastSuite is absent or its
     * optional API bridge is unavailable and the caller should use a complete
     * Forge registry scan.
     */
    @Nullable
    public static List<IRecipe> findAllMatches(InventoryCrafting matrix, World world) {
        if (matrix == null || world == null) {
            return null;
        }
        if (!ensureCurrentApi()) {
            return null;
        }

        final Object rawCandidates;
        try {
            rawCandidates = getCandidateRecipes.invoke(null, matrix);
        } catch (IllegalAccessException | LinkageError | RuntimeException exception) {
            reportFailure(exception);
            return null;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            reportFailure(cause == null ? exception : cause);
            return null;
        }

        if (!(rawCandidates instanceof Iterable)) {
            reportFailure(new IllegalStateException(
                    "FastSuite candidate API returned "
                            + (rawCandidates == null ? "null" : rawCandidates.getClass().getName())));
            return null;
        }

        List<IRecipe> matches = new ArrayList<IRecipe>(4);
        try {
            for (Object candidate : (Iterable<?>) rawCandidates) {
                if (candidate instanceof IRecipe
                        && RecipeProbe.matches((IRecipe) candidate, matrix, world)) {
                    matches.add((IRecipe) candidate);
                }
            }
        } catch (LinkageError | RuntimeException exception) {
            // Fail open if the candidate iterable itself is broken. Individual
            // recipe.matches() failures are already isolated by RecipeProbe.
            reportFailure(exception);
            return null;
        }

        lastMode = Mode.FASTSUITE_CANDIDATES;
        return matches.isEmpty()
                ? Collections.<IRecipe>emptyList()
                : Collections.unmodifiableList(matches);
    }

    public static Mode getLastMode() {
        return lastMode;
    }

    public static long getFailureCount() {
        return FAILURES.get();
    }

    public static void noteForgeScan() {
        lastMode = Mode.FORGE_REGISTRY;
    }

    public static void resetDiagnostics() {
        FAILURES.set(0L);
        REPORTED_FAILURES.clear();
    }

    private static boolean ensureCurrentApi() {
        if (initialized) {
            return getCandidateRecipes != null;
        }

        synchronized (FastSuiteInterop.class) {
            if (initialized) {
                return getCandidateRecipes != null;
            }

            ClassLoader loader = FastSuiteInterop.class.getClassLoader();
            Throwable lastFailure = null;
            boolean apiClassFound = false;
            for (String className : API_CLASSES) {
                try {
                    Class<?> api = Class.forName(className, false, loader);
                    apiClassFound = true;
                    Method candidates;
                    try {
                        candidates = api.getMethod("getCandidateRecipes", InventoryCrafting.class);
                    } catch (NoSuchMethodException missingCurrentName) {
                        // Compatibility with the early alpha API while keeping
                        // the current release package as the primary path.
                        candidates = api.getMethod("getCraftingCandidates", InventoryCrafting.class);
                    }
                    getCandidateRecipes = candidates;
                    resolvedApiClass = className;
                    LOGGER.info(
                            "Retro FastSuite detected; RetroPolymorph will use {}#{} for conflict candidate scans",
                            className,
                            candidates.getName());
                    break;
                } catch (ClassNotFoundException absent) {
                    lastFailure = absent;
                } catch (NoSuchMethodException | LinkageError | RuntimeException exception) {
                    apiClassFound = true;
                    lastFailure = exception;
                }
            }

            initialized = true;
            // FastSuite is optional. Missing API classes are the normal absent-mod
            // case and should not produce a warning/failure counter. If one of
            // the known API classes exists but its contract is unusable, report it.
            if (getCandidateRecipes == null && apiClassFound && lastFailure != null) {
                reportFailure(lastFailure);
            }
            return getCandidateRecipes != null;
        }
    }

    private static void reportFailure(Throwable exception) {
        FAILURES.incrementAndGet();
        String cause = exception == null ? "<unknown>" : exception.getClass().getName();
        if (REPORTED_FAILURES.putIfAbsent(cause, Boolean.TRUE) != null) {
            return;
        }
        LOGGER.warn(
                "Retro FastSuite candidate integration failed; using complete Forge recipe scan for correctness (api={}, cause={})",
                resolvedApiClass == null ? "unresolved" : resolvedApiClass,
                exception == null ? "unknown failure" : exception.toString());
        LOGGER.debug("Retro FastSuite integration failure details", exception);
    }
}
