package dev.sosea1.retropolymorph.compat.tconstruct;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class TinkersSharedSelectionRegistryTest {

    @Test
    public void viewersOfSamePersistentParentShareSelectionUntilLastClose() {
        IInventory parent = new InventoryBasic("station", false, 9);
        ProbeContainer first = new ProbeContainer();
        ProbeContainer second = new ProbeContainer();
        TestMatrix firstMatrix = new TestMatrix(first, parent);
        TestMatrix secondMatrix = new TestMatrix(second, parent);

        try {
            TinkersSharedSelectionRegistry.bind(first, firstMatrix);
            TinkersSharedSelectionRegistry.bind(second, secondMatrix);

            ResourceLocation wanted = new ResourceLocation("example", "shared_recipe");
            TinkersSharedSelectionRegistry.select(first, firstMatrix, wanted);

            assertSame(wanted, TinkersSharedSelectionRegistry.selected(first, firstMatrix));
            assertSame(wanted, TinkersSharedSelectionRegistry.selected(second, secondMatrix));
            assertSame(wanted, secondMatrix.retropolymorph$peekRecipeSelectionState()
                    .getSelectedRecipeId());
            assertEquals(1, TinkersSharedSelectionRegistry.activeStationCount());

            TinkersSharedSelectionRegistry.onContainerClosed(first);
            assertEquals(1, TinkersSharedSelectionRegistry.activeStationCount());
            assertSame(wanted, TinkersSharedSelectionRegistry.selected(second, secondMatrix));
        } finally {
            TinkersSharedSelectionRegistry.onContainerClosed(first);
            TinkersSharedSelectionRegistry.onContainerClosed(second);
        }

        assertEquals(0, TinkersSharedSelectionRegistry.activeStationCount());
    }

    @Test
    public void differentPersistentParentsNeverShareSelection() {
        ProbeContainer first = new ProbeContainer();
        ProbeContainer second = new ProbeContainer();
        TestMatrix firstMatrix = new TestMatrix(
                first, new InventoryBasic("station-a", false, 9));
        TestMatrix secondMatrix = new TestMatrix(
                second, new InventoryBasic("station-b", false, 9));

        try {
            TinkersSharedSelectionRegistry.bind(first, firstMatrix);
            TinkersSharedSelectionRegistry.bind(second, secondMatrix);
            TinkersSharedSelectionRegistry.select(
                    first, firstMatrix, new ResourceLocation("example", "only_first"));

            assertFalse(TinkersSharedSelectionRegistry.sameStation(first, second));
            assertNull(TinkersSharedSelectionRegistry.selected(second, secondMatrix));
        } finally {
            TinkersSharedSelectionRegistry.onContainerClosed(first);
            TinkersSharedSelectionRegistry.onContainerClosed(second);
        }
    }

    @Test
    public void refreshDoesNotHoldRegistryMonitorAcrossContainerCallbacks() throws Exception {
        IInventory parent = new InventoryBasic("station", false, 9);
        ProbeContainer source = new ProbeContainer();
        ProbeContainer peer = new ProbeContainer();
        TestMatrix sourceMatrix = new TestMatrix(source, parent);
        TestMatrix peerMatrix = new TestMatrix(peer, parent);

        try {
            TinkersSharedSelectionRegistry.bind(source, sourceMatrix);
            TinkersSharedSelectionRegistry.bind(peer, peerMatrix);

            peer.probeRegistryLockDuringRefresh = true;
            TinkersSharedSelectionRegistry.refreshPeers(source);

            assertEquals(1, peer.matrixChangeCalls);
            assertEquals(1, peer.detectCalls);
            assertFalse(peer.registryBlockedDuringCallback);
        } finally {
            TinkersSharedSelectionRegistry.onContainerClosed(source);
            TinkersSharedSelectionRegistry.onContainerClosed(peer);
        }
    }

    private static final class TestMatrix extends InventoryCrafting
            implements TinkersPersistentMatrixAccess, CraftingMatrixExtension {

        private final IInventory parent;
        private final Container owner;

        @Nullable
        private RecipeSelectionState state;

        private TestMatrix(Container owner, IInventory parent) {
            super(owner, 3, 3);
            this.owner = owner;
            this.parent = parent;
        }

        @Override
        public IInventory retropolymorph$getPersistentParent() {
            return this.parent;
        }

        @Override
        @Nullable
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

        @Override
        public Container retropolymorph$getCraftingOwner() {
            return this.owner;
        }
    }

    private static final class ProbeContainer extends Container
            implements TinkersCraftingStationAccess {

        private int matrixChangeCalls;
        private int detectCalls;
        private boolean probeRegistryLockDuringRefresh;
        private volatile boolean registryBlockedDuringCallback;

        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }

        @Override
        public void onCraftMatrixChanged(IInventory inventoryIn) {
            this.matrixChangeCalls++;
            if (!this.probeRegistryLockDuringRefresh) {
                return;
            }

            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    TinkersSharedSelectionRegistry.activeStationCount();
                }
            }, "retropolymorph-registry-lock-probe");
            worker.start();
            try {
                worker.join(250L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
            this.registryBlockedDuringCallback = worker.isAlive();
            if (worker.isAlive()) {
                try {
                    worker.join(1000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
            }
        }

        @Override
        public void detectAndSendChanges() {
            this.detectCalls++;
            super.detectAndSendChanges();
        }

        @Override
        public void retropolymorph$clearLastRecipe() {
            // The registry only needs to prove it can invalidate before refresh.
        }
    }
}
