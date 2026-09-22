package dev.sosea1.retropolymorph.machine;

import dev.sosea1.retropolymorph.api.AdapterContractTestBase;
import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.MachineRecipeAdapter;
import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionAdapter;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MachineRecipeSelectionAdapterContractTest extends AdapterContractTestBase<MachineRecipeSelectionAdapterContractTest.TestMachineContainer> {

    public static class TestMachineContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    }

    public static class OtherContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    }

    private static class StubSurface implements MachineRecipeSurface {
        private final Container container;
        private final boolean controlsOperation;
        private final Slot resultSlot;
        private final Item testItem = new Item();
        private String selectedKey = null;

        StubSurface(Container container, boolean controlsOperation) {
            this.container = container;
            this.controlsOperation = controlsOperation;
            this.resultSlot = new Slot(new InventoryBasic("result", false, 1), 0, 80, 35);
        }

        @Override
        public Container getContainer() {
            return this.container;
        }

        @Override
        public Object getRecipeOwner() {
            return this.container;
        }

        @Override
        public int getInputCount() {
            return 1;
        }

        @Override
        public ItemStack getInputStack(int index) {
            return index == 0 ? new ItemStack(this.testItem) : ItemStack.EMPTY;
        }

        @Override
        public int getClientStateToken() {
            return 0;
        }

        @Override
        public boolean controlsActualOperation() {
            return this.controlsOperation;
        }

        @Override
        public MachineRecipePersistence getPersistencePolicy() {
            return MachineRecipePersistence.PLAYER_PROFILE;
        }

        @Override
        public List<RecipeOption> findOptions(World world) {
            List<RecipeOption> list = new ArrayList<RecipeOption>();
            list.add(new RecipeOption("mod:recipe_a", new ItemStack(this.testItem)));
            list.add(new RecipeOption("mod:recipe_b", new ItemStack(this.testItem)));
            return list;
        }

        @Override
        public boolean selectRecipe(String recipeKey, World world) {
            if ("mod:recipe_a".equals(recipeKey) || "mod:recipe_b".equals(recipeKey)) {
                this.selectedKey = recipeKey;
                return true;
            }
            return false;
        }

        @Override
        public void clearRecipeSelection() {
            this.selectedKey = null;
        }

        @Nullable
        @Override
        public String getSelectedRecipeKey() {
            return this.selectedKey;
        }

        @Override
        public Slot getResultSlot() {
            return this.resultSlot;
        }
    }

    private final MachineRecipeAdapter stubAdapter = new MachineRecipeAdapter() {
        @Override
        public boolean recognizes(Container container) {
            return container instanceof TestMachineContainer;
        }

        @Override
        public MachineRecipeSurface openSurface(Container container) {
            if (container instanceof TestMachineContainer) {
                return new StubSurface(container, true);
            }
            return null;
        }
    };

    @Override
    protected RecipeSelectionAdapter createAdapter() {
        return new MachineRecipeSelectionAdapter(stubAdapter);
    }

    @Override
    protected TestMachineContainer createMatchingContainer() {
        return new TestMachineContainer();
    }

    @Override
    protected Container createUnrelatedContainer() {
        return new OtherContainer();
    }

    @Test
    public void cosmeticOnlySurfaceIsRejected() {
        MachineRecipeAdapter cosmeticAdapter = new MachineRecipeAdapter() {
            @Override
            public boolean recognizes(Container container) {
                return container instanceof TestMachineContainer;
            }

            @Override
            public MachineRecipeSurface openSurface(Container container) {
                return new StubSurface(container, false); // controlsActualOperation = false!
            }
        };

        MachineRecipeSelectionAdapter adapter = new MachineRecipeSelectionAdapter(cosmeticAdapter);
        TestMachineContainer container = new TestMachineContainer();

        AdapterDetectionResult result = adapter.probe(container);
        assertFalse(result.isMatch(), "A surface that fails controlsActualOperation must not be adapted");
        assertTrue(result.isBlockFallback(), "A surface that fails controlsActualOperation must block fallback");
    }
}
