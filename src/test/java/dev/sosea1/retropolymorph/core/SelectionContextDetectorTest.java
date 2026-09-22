package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapters;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SelectionContextDetectorTest {

    @Test
    public void blockFallbackRemainsDistinctFromMissWhenAContainerStateChanges() {
        AtomicBoolean active = new AtomicBoolean(false);
        RecipeSelectionAdapters.register(
                new ResourceLocation("retropolymorph_test", "temporary_adapter"),
                10000,
                new RecipeSelectionAdapter() {
                    @Override
                    public AdapterDetectionResult probe(Container container) {
                        return active.get()
                                ? AdapterDetectionResult.match(new EmptyContext(container))
                                : AdapterDetectionResult.blockFallback();
                    }
                });
        Container container = new TestContainer();

        assertTrue(SelectionContextDetector.probe(container).isBlockFallback());
        active.set(true);
        assertTrue(SelectionContextDetector.probe(container).isMatch());
    }

    private static final class TestContainer extends Container {
        @Override public boolean canInteractWith(EntityPlayer playerIn) { return true; }
    }

    private static final class EmptyContext implements SelectionContext {
        private final Container container;
        private EmptyContext(Container container) { this.container = container; }
        @Override public Container getContainer() { return this.container; }
        @Override public net.minecraft.inventory.Slot getResultSlot() { return null; }
        @Override public int getInputCount() { return 0; }
        @Override public net.minecraft.item.ItemStack getInputStack(int index) { return net.minecraft.item.ItemStack.EMPTY; }
        @Override public java.util.List<dev.sosea1.retropolymorph.api.RecipeOption> findOptions(net.minecraft.world.World world) { return java.util.Collections.emptyList(); }
        @Override public boolean select(String recipeKey, net.minecraft.world.World world) { return false; }
        @Override public void clearSelection() { }
        @Override public String getSelectedRecipeKey() { return null; }
    }
}
