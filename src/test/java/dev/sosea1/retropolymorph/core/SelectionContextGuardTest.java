package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SelectionContextGuardTest {

    @AfterEach
    public void reset() {
        SelectionContextGuard.resetDiagnostics();
    }

    @Test
    public void failingIntegrationIsContainedInsteadOfEscaping() {
        SelectionContext context = new ThrowingContext();

        assertNull(SelectionContextGuard.findOptions(context, null));
        assertFalse(SelectionContextGuard.select(context, "example:recipe", null));
        SelectionContextGuard.clear(context);
        assertNull(SelectionContextGuard.selected(context));

        assertTrue(SelectionContextGuard.getFailureCount() >= 4L);
        assertTrue(SelectionContextGuard.getReportedContextClassCount() == 1);
    }

    private static final class ThrowingContext implements SelectionContext {

        @Override
        public Container getContainer() {
            return null;
        }

        @Override
        public Slot getResultSlot() {
            return null;
        }

        @Override
        public int getInputCount() {
            return 0;
        }

        @Override
        public ItemStack getInputStack(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public List<RecipeOption> findOptions(World world) {
            throw new IllegalStateException("broken integration");
        }

        @Override
        public boolean select(String recipeKey, World world) {
            throw new IllegalStateException("broken integration");
        }

        @Override
        public void clearSelection() {
            throw new IllegalStateException("broken integration");
        }

        @Override
        @Nullable
        public String getSelectedRecipeKey() {
            throw new IllegalStateException("broken integration");
        }
    }
}
