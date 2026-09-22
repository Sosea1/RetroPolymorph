package dev.sosea1.retropolymorph.compat.fastsuite;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optional bridge to the current FastSuite112 all-match API.
 *
 * <p>There are deliberately only two modes:</p>
 * <ul>
 *     <li>FastSuite installed: use {@code FastSuiteAPI.visitMatchingRecipes(...)}.</li>
 *     <li>FastSuite absent: caller performs the normal complete Forge registry scan.</li>
 * </ul>
 *
 * <p>RetroPolymorph is developed together with FastSuite, so old FastSuite API generations are not
 * supported here. Reflection exists only so FastSuite remains an optional runtime dependency.</p>
 */
public final class FastSuiteInterop {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final String MOD_ID = "fastsuite112";
    private static final String API_CLASS = "dev.sosea1.fastsuite112.api.FastSuiteAPI";
    private static final String VISITOR_CLASS = "dev.sosea1.fastsuite112.api.RecipeMatchVisitor";

    private static final AtomicLong FAILURES = new AtomicLong();
    private static final ConcurrentMap<String, Boolean> REPORTED_FAILURES =
            new ConcurrentHashMap<String, Boolean>();

    private static volatile boolean initialized;
    private static volatile Method visitMatchingRecipes;
    private static volatile Class<?> visitorType;
    private static volatile Mode lastMode = Mode.FORGE_REGISTRY;

    private FastSuiteInterop() {
    }

    public enum Mode {
        FASTSUITE_VISITOR,
        FORGE_REGISTRY
    }

    public static boolean isInstalled() {
        return Loader.isModLoaded(MOD_ID);
    }

    /**
     * Use the current FastSuite ordered all-match visitor when FastSuite is installed.
     *
     * @return complete matching recipes in Forge registry order; null when FastSuite is absent or
     * the current integration failed and the caller should use the complete Forge registry scan.
     */
    @Nullable
    public static List<IRecipe> findAllMatches(InventoryCrafting matrix, World world) {
        if (matrix == null || world == null || !isInstalled()) {
            return null;
        }

        if (!ensureCurrentApi()) {
            return null;
        }

        final List<IRecipe> matches = new ArrayList<IRecipe>(4);
        try {
            Object visitor = Proxy.newProxyInstance(
                    visitorType.getClassLoader(),
                    new Class<?>[] { visitorType },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("visit".equals(name)
                                    && args != null
                                    && args.length == 1
                                    && args[0] instanceof IRecipe) {
                                matches.add((IRecipe) args[0]);
                                return Boolean.TRUE;
                            }
                            if (method.getDeclaringClass() == Object.class) {
                                if ("toString".equals(name)) {
                                    return "RetroPolymorphFastSuiteVisitor";
                                }
                                if ("hashCode".equals(name)) {
                                    return System.identityHashCode(proxy);
                                }
                                if ("equals".equals(name)) {
                                    return args != null && args.length == 1 && proxy == args[0];
                                }
                            }
                            throw new UnsupportedOperationException("Unexpected FastSuite visitor method: " + method);
                        }
                    });

            visitMatchingRecipes.invoke(null, matrix, world, visitor);
            lastMode = Mode.FASTSUITE_VISITOR;
            return matches.isEmpty()
                    ? Collections.<IRecipe>emptyList()
                    : Collections.unmodifiableList(new ArrayList<IRecipe>(matches));
        } catch (IllegalAccessException | LinkageError | RuntimeException exception) {
            reportFailure(exception);
            return null;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            reportFailure(cause == null ? exception : cause);
            return null;
        }
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
            return visitMatchingRecipes != null && visitorType != null;
        }

        synchronized (FastSuiteInterop.class) {
            if (initialized) {
                return visitMatchingRecipes != null && visitorType != null;
            }

            try {
                ClassLoader loader = FastSuiteInterop.class.getClassLoader();
                Class<?> api = Class.forName(API_CLASS, false, loader);
                visitorType = Class.forName(VISITOR_CLASS, false, loader);
                visitMatchingRecipes = api.getMethod(
                        "visitMatchingRecipes",
                        InventoryCrafting.class,
                        World.class,
                        visitorType);
                LOGGER.info("FastSuite112 detected; RetroPolymorph will use visitMatchingRecipes for conflict scans");
            } catch (ClassNotFoundException | NoSuchMethodException | LinkageError | RuntimeException exception) {
                visitMatchingRecipes = null;
                visitorType = null;
                reportFailure(exception);
            } finally {
                initialized = true;
            }

            return visitMatchingRecipes != null && visitorType != null;
        }
    }

    private static void reportFailure(Throwable exception) {
        FAILURES.incrementAndGet();
        String cause = exception == null ? "<unknown>" : exception.getClass().getName();
        if (REPORTED_FAILURES.putIfAbsent(cause, Boolean.TRUE) != null) {
            return;
        }
        LOGGER.warn(
                "Current FastSuite112 visitMatchingRecipes integration failed; using complete Forge recipe scan for correctness ({})",
                exception == null ? "unknown failure" : exception.toString());
        LOGGER.debug("FastSuite112 integration failure details", exception);
    }
}
