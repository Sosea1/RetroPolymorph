package dev.sosea1.retropolymorph.machine;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.MachineRecipeAdapter;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Internal bridge from the public machine API into the common selector pipeline. */
public final class MachineRecipeSelectionAdapter implements RecipeSelectionAdapter {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final ConcurrentMap<String, Boolean> REPORTED =
            new ConcurrentHashMap<String, Boolean>();

    private final MachineRecipeAdapter delegate;

    public MachineRecipeSelectionAdapter(MachineRecipeAdapter delegate) {
        if (delegate == null) {
            throw new NullPointerException("delegate");
        }
        this.delegate = delegate;
    }

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (container == null) {
            return AdapterDetectionResult.miss();
        }
        if (!safeRecognizes(container)) {
            return AdapterDetectionResult.miss();
        }

        MachineRecipeSurface surface;
        try {
            surface = this.delegate.openSurface(container);
        } catch (RuntimeException | LinkageError exception) {
            reportOnce("open", container, exception);
            return AdapterDetectionResult.blockFallback();
        }
        if (surface == null) {
            return AdapterDetectionResult.blockFallback();
        }
        try {
            if (surface.getContainer() != container) {
                reportOnce("foreign-container", container, null);
                return AdapterDetectionResult.blockFallback();
            }
            if (!surface.controlsActualOperation()) {
                reportOnce("cosmetic-only", container, null);
                return AdapterDetectionResult.blockFallback();
            }
            if (!surface.getSelectorPlacement().isVisible()) {
                reportOnce("missing-anchor", container, null);
                return AdapterDetectionResult.blockFallback();
            }
            return AdapterDetectionResult.match(new MachineSelectionContext(surface));
        } catch (RuntimeException | LinkageError exception) {
            reportOnce("validate-surface", container, exception);
            return AdapterDetectionResult.blockFallback();
        }
    }


    private boolean safeRecognizes(Container container) {
        try {
            return this.delegate.recognizes(container);
        } catch (RuntimeException | LinkageError exception) {
            reportOnce("recognize", container, exception);
            return false;
        }
    }

    private void reportOnce(String operation, Container container, @Nullable Throwable exception) {
        String containerClass = container == null ? "<null>" : container.getClass().getName();
        String key = this.delegate.getClass().getName() + "|" + operation + "|" + containerClass;
        if (REPORTED.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }
        LOGGER.warn(
                "Machine recipe adapter {} rejected {} during {}. Generic fallback remains blocked when the custom engine was recognized.{}",
                this.delegate.getClass().getName(),
                containerClass,
                operation,
                exception == null ? "" : " Cause: " + exception.toString());
        if (exception != null) {
            LOGGER.debug("Machine recipe adapter failure details", exception);
        }
    }
}
