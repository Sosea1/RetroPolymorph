package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-side failure boundary around optional integration contexts.
 *
 * <p>An adapter is an enhancement, not a prerequisite for the owning machine to
 * function. If a third-party update or reflection mismatch makes a context throw,
 * fail open: stop applying RetroPolymorph selection for that request and let the
 * native container keep its own behavior.</p>
 */
public final class SelectionContextGuard {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final AtomicLong FAILURES = new AtomicLong();
    private static final ConcurrentMap<String, Boolean> REPORTED =
            new ConcurrentHashMap<String, Boolean>();
    private static final ConcurrentMap<String, Boolean> BROKEN_CLASSES =
            new ConcurrentHashMap<String, Boolean>();

    private SelectionContextGuard() {
    }

    @Nullable
    public static List<RecipeOption> findOptions(SelectionContext context, World world) {
        try {
            return context.findOptions(world);
        } catch (RuntimeException | LinkageError exception) {
            report("findOptions", context, exception);
            return null;
        }
    }

    public static boolean select(SelectionContext context, String recipeKey, World world) {
        try {
            return context.select(recipeKey, world);
        } catch (RuntimeException | LinkageError exception) {
            report("select", context, exception);
            return false;
        }
    }

    public static void clear(SelectionContext context) {
        try {
            context.clearSelection();
        } catch (RuntimeException | LinkageError exception) {
            report("clear", context, exception);
        }
    }

    @Nullable
    public static String selected(SelectionContext context) {
        try {
            return context.getSelectedRecipeKey();
        } catch (RuntimeException | LinkageError exception) {
            report("getSelectedRecipeKey", context, exception);
            return null;
        }
    }

    public static long getFailureCount() {
        return FAILURES.get();
    }

    public static int getReportedContextClassCount() {
        return BROKEN_CLASSES.size();
    }

    public static void resetDiagnostics() {
        FAILURES.set(0L);
        REPORTED.clear();
        BROKEN_CLASSES.clear();
    }

    private static void report(String operation, SelectionContext context, Throwable exception) {
        FAILURES.incrementAndGet();
        String className = context == null ? "<null>" : context.getClass().getName();
        BROKEN_CLASSES.putIfAbsent(className, Boolean.TRUE);
        String key = operation + "|" + className;
        if (REPORTED.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        LOGGER.warn(
                "Selection context {} failed in {}. RetroPolymorph will fail open for this request. Cause: {}",
                className,
                operation,
                exception == null ? "<unknown>" : exception.toString());
        LOGGER.debug("Selection context failure details", exception);
    }
}
