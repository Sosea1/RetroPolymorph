package dev.sosea1.retropolymorph.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that SelectionContext V2 implementations providing only getSelectorPlacement()
 * and getPersistencePolicy() function completely without implementing legacy deprecated methods.
 */
public class SelectorPlacementV2AdoptionTest {

    private static final class MinimalV2VisibleContext implements SelectionContext {
        private final Container container = new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        };

        @Override
        public Container getContainer() {
            return this.container;
        }

        @Nullable
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
            return Collections.emptyList();
        }

        @Override
        public boolean select(String recipeKey, World world) {
            return false;
        }

        @Override
        public void clearSelection() {
        }

        @Nullable
        @Override
        public String getSelectedRecipeKey() {
            return null;
        }

        @Override
        public SelectorPlacement getSelectorPlacement() {
            return SelectorPlacement.absolute(100, 50, 2, -3);
        }

        @Override
        public SelectionPersistencePolicy getPersistencePolicy() {
            return SelectionPersistencePolicy.OWNER_ONLY;
        }
    }

    private static final class MinimalV2HiddenContext implements SelectionContext {
        private final Container container = new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        };

        @Override
        public Container getContainer() {
            return this.container;
        }

        @Nullable
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
            return Collections.emptyList();
        }

        @Override
        public boolean select(String recipeKey, World world) {
            return false;
        }

        @Override
        public void clearSelection() {
        }

        @Nullable
        @Override
        public String getSelectedRecipeKey() {
            return null;
        }

        @Override
        public SelectorPlacement getSelectorPlacement() {
            return SelectorPlacement.hidden();
        }

        @Override
        public SelectionPersistencePolicy getPersistencePolicy() {
            return SelectionPersistencePolicy.PLAYER_PERSISTENT;
        }
    }

    @Test
    public void testVisibleV2ContextIsRecognizedSolelyThroughSelectorPlacement() {
        SelectionContext context = new MinimalV2VisibleContext();

        // Modern V2 API checks
        SelectorPlacement placement = context.getSelectorPlacement();
        assertTrue(placement.isVisible());
        assertEquals(SelectorPlacement.AnchorMode.ABSOLUTE, placement.getMode());
        assertEquals(100, placement.getAnchorX());
        assertEquals(50, placement.getAnchorY());
        assertEquals(2, placement.getOffsetX());
        assertEquals(-3, placement.getOffsetY());
        assertEquals(SelectionPersistencePolicy.OWNER_ONLY, context.getPersistencePolicy());
    }

    @Test
    public void testHiddenV2ContextReportsHidden() {
        SelectionContext context = new MinimalV2HiddenContext();

        SelectorPlacement placement = context.getSelectorPlacement();
        assertFalse(placement.isVisible());
        assertEquals(SelectorPlacement.AnchorMode.HIDDEN, placement.getMode());
        assertEquals(SelectionPersistencePolicy.PLAYER_PERSISTENT, context.getPersistencePolicy());
    }
}
