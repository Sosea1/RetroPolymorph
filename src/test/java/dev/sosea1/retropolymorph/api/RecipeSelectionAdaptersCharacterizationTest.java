package dev.sosea1.retropolymorph.api;

import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RecipeSelectionAdaptersCharacterizationTest {

    @BeforeAll
    public static void setup() {
        Bootstrap.register();
    }

    @AfterEach
    public void tearDown() {
        RecipeSelectionAdapters.resetForTests();
    }

    @Test
    public void highestPriorityMatchingAdapterWins() {
        TestContainer container = new TestContainer();
        final SelectionContext lowCtx = new TestContext(container);
        final SelectionContext highCtx = new TestContext(container);

        RecipeSelectionAdapters.register(
                new ResourceLocation("test:low"),
                10,
                new TestAdapter(lowCtx, false));
        RecipeSelectionAdapters.register(
                new ResourceLocation("test:high"),
                100,
                new TestAdapter(highCtx, false));

        SelectionContext detected = RecipeSelectionAdapters.detect(container);
        assertNotNull(detected);
        assertEquals(highCtx, detected);
    }

    @Test
    public void registrationOrderBreaksEqualPriorityTies() {
        TestContainer container = new TestContainer();
        final SelectionContext firstCtx = new TestContext(container);
        final SelectionContext secondCtx = new TestContext(container);

        RecipeSelectionAdapters.register(
                new ResourceLocation("test:first"),
                50,
                new TestAdapter(firstCtx, false));
        RecipeSelectionAdapters.register(
                new ResourceLocation("test:second"),
                50,
                new TestAdapter(secondCtx, false));

        SelectionContext detected = RecipeSelectionAdapters.detect(container);
        assertNotNull(detected);
        assertEquals(firstCtx, detected);
    }

    @Test
    public void guardAdapterBlocksFallbackDetection() {
        TestContainer container = new TestContainer();

        RecipeSelectionAdapters.register(
                new ResourceLocation("test:guard"),
                100,
                new TestAdapter(null, true));

        assertNull(RecipeSelectionAdapters.detect(container));
        assertTrue(RecipeSelectionAdapters.blocksFallbackDetection(container));
        assertNull(SelectionContextDetector.detect(container));
    }

    @Test
    public void unrelatedAdapterDoesNotBlockFallback() {
        TestContainer container = new TestContainer();

        RecipeSelectionAdapters.register(
                new ResourceLocation("test:unrelated"),
                100,
                new TestAdapter(null, false));

        assertNull(RecipeSelectionAdapters.detect(container));
        assertFalse(RecipeSelectionAdapters.blocksFallbackDetection(container));
    }

    private static final class TestContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    }

    private static final class TestAdapter implements RecipeSelectionAdapter {
        private final SelectionContext contextToReturn;
        private final boolean blockFallback;

        TestAdapter(@Nullable SelectionContext contextToReturn, boolean blockFallback) {
            this.contextToReturn = contextToReturn;
            this.blockFallback = blockFallback;
        }

        @Override
        public AdapterDetectionResult probe(Container container) {
            if (this.contextToReturn != null) {
                return AdapterDetectionResult.match(this.contextToReturn);
            }
            return this.blockFallback
                    ? AdapterDetectionResult.blockFallback()
                    : AdapterDetectionResult.miss();
        }
    }

    private static final class TestContext implements SelectionContext {
        private final Container container;

        TestContext(Container container) {
            this.container = container;
        }

        @Override
        public Container getContainer() {
            return this.container;
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
    }
}
