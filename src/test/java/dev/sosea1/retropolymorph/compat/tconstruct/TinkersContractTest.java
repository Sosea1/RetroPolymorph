package dev.sosea1.retropolymorph.compat.tconstruct;

import dev.sosea1.retropolymorph.api.AdapterContractTestBase;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionScope;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.tools.common.inventory.ContainerCraftingStation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinkersContractTest extends AdapterContractTestBase<ContainerCraftingStation> {

    public static class UnrelatedContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    }

    private ContainerCraftingStation activeContainer;

    @AfterEach
    public void tearDown() {
        if (this.activeContainer != null) {
            TinkersSharedSelectionRegistry.onContainerClosed(this.activeContainer);
        }
    }

    @Override
    protected RecipeSelectionAdapter createAdapter() {
        return TinkersCraftingStationAdapter.INSTANCE;
    }

    @Override
    protected ContainerCraftingStation createMatchingContainer() {
        this.activeContainer = new ContainerCraftingStation();
        return this.activeContainer;
    }

    @Override
    protected Container createUnrelatedContainer() {
        return new UnrelatedContainer();
    }

    @Test
    public void tinkersSelectionScopeIsShared() {
        ContainerCraftingStation container = createMatchingContainer();
        SelectionContext context = createAdapter().probe(container).getContext();
        assertNotNull(context);

        SelectionScope scope = context.getSelectionScope();
        assertNotNull(scope);
        assertTrue(scope.isShared(), "Tinkers crafting station context must have shared selection scope");
        assertEquals(container.getMatrix().retropolymorph$getPersistentParent(), scope.getOwnerIdentity());
    }
}
