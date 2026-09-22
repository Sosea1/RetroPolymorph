package slimeknights.tconstruct.tools.common.inventory;

import dev.sosea1.retropolymorph.compat.tconstruct.TinkersSharedSelectionRegistry;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import dev.sosea1.retropolymorph.mixin.SlotCraftingAccessor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Test stub representing Tinkers' ContainerCraftingStation for contract tests.
 */
public class ContainerCraftingStation extends Container {

    public static class StubMatrix extends InventoryCrafting implements CraftingMatrixExtension, dev.sosea1.retropolymorph.compat.tconstruct.TinkersPersistentMatrixAccess {
        private final Container container;
        private final IInventory persistentParent;
        private RecipeSelectionState state;

        public StubMatrix(Container container, IInventory persistentParent) {
            super(container, 3, 3);
            this.container = container;
            this.persistentParent = persistentParent;
        }

        @Override
        public IInventory retropolymorph$getPersistentParent() {
            return this.persistentParent;
        }

        @Override
        public Container retropolymorph$getCraftingOwner() {
            return this.container;
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

    public static class StubSlotCrafting extends SlotCrafting implements SlotCraftingAccessor {
        private final InventoryCrafting matrix;

        public StubSlotCrafting(InventoryCrafting matrix, IInventory result, int index, int x, int y) {
            super(null, matrix, result, index, x, y);
            this.matrix = matrix;
        }

        @Override
        public InventoryCrafting retropolymorph$getCraftMatrix() {
            return this.matrix;
        }
    }

    private final StubMatrix matrix;
    private final StubSlotCrafting resultSlot;

    public ContainerCraftingStation() {
        InventoryBasic parent = new InventoryBasic("station", false, 9);
        this.matrix = new StubMatrix(this, parent);
        InventoryCraftResult result = new InventoryCraftResult();
        this.resultSlot = new StubSlotCrafting(this.matrix, result, 0, 80, 35);
        this.addSlotToContainer(this.resultSlot);
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(this.matrix, i, 10 + (i % 3) * 18, 10 + (i / 3) * 18));
        }
    }

    public StubMatrix getMatrix() {
        return this.matrix;
    }

    public StubSlotCrafting getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
}
