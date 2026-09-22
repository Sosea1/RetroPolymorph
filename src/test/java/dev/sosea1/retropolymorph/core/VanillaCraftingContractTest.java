package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.AdapterContractTestBase;
import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectionScope;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.mixin.SlotCraftingAccessor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VanillaCraftingContractTest extends AdapterContractTestBase<VanillaCraftingContractTest.TestWorkbenchContainer> {

    public static class TestMatrix extends InventoryCrafting implements CraftingMatrixExtension {
        private final Container owner;
        private RecipeSelectionState state;

        public TestMatrix(Container owner) {
            super(owner, 3, 3);
            this.owner = owner;
        }

        @Override
        public Container retropolymorph$getCraftingOwner() {
            return this.owner;
        }

        @Nullable
        @Override
        public RecipeSelectionState retropolymorph$peekRecipeSelectionState() {
            return this.state;
        }

        @Override
        public RecipeSelectionState retropolymorph$getOrCreateRecipeSelectionState() {
            if (this.state == null) {
                this.state = new RecipeSelectionState();
            }
            return this.state;
        }
    }

    public static class TestSlotCrafting extends SlotCrafting implements SlotCraftingAccessor {
        private final InventoryCrafting matrix;

        public TestSlotCrafting(InventoryCrafting matrix, IInventory result, int index, int x, int y) {
            super(null, matrix, result, index, x, y);
            this.matrix = matrix;
        }

        @Override
        public InventoryCrafting retropolymorph$getCraftMatrix() {
            return this.matrix;
        }
    }

    public static class TestWorkbenchContainer extends Container {
        final TestMatrix matrix = new TestMatrix(this);
        final InventoryCraftResult result = new InventoryCraftResult();
        final TestSlotCrafting resultSlot;

        public TestWorkbenchContainer() {
            this.resultSlot = new TestSlotCrafting(this.matrix, this.result, 0, 80, 35);
            this.addSlotToContainer(this.resultSlot);
            for (int i = 0; i < 9; i++) {
                this.addSlotToContainer(new Slot(this.matrix, i, 10 + (i % 3) * 18, 10 + (i / 3) * 18));
            }
        }

        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    }

    public static class EmptyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    }

    private final RecipeSelectionAdapter genericCraftingAdapter = container -> {
        SelectionContext context = CraftingContextDetector.detect(container);
        return context != null ? AdapterDetectionResult.match(context) : AdapterDetectionResult.miss();
    };

    @Override
    protected RecipeSelectionAdapter createAdapter() {
        return this.genericCraftingAdapter;
    }

    @Override
    protected TestWorkbenchContainer createMatchingContainer() {
        return new TestWorkbenchContainer();
    }

    @Override
    protected Container createUnrelatedContainer() {
        return new EmptyContainer();
    }

    @Test
    public void vanillaCraftingHasLocalScopeAndPlayerPersistence() {
        TestWorkbenchContainer container = createMatchingContainer();
        SelectionContext context = createAdapter().probe(container).getContext();
        assertNotNull(context);

        SelectionScope scope = context.getSelectionScope();
        assertNotNull(scope);
        assertFalse(scope.isShared(), "Vanilla crafting must have local scope");
        assertEquals(SelectionPersistencePolicy.PLAYER_PERSISTENT, context.getPersistencePolicy());
        assertEquals(SelectorPlacement.AnchorMode.RESULT_SLOT, context.getSelectorPlacement().getMode());
    }
}
