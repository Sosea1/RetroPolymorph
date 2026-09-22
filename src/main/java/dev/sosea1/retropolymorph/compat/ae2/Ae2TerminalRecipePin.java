package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pins the visible/server-side AE2 crafting result to the recipe explicitly
 * selected by RetroPolymorph.
 *
 * <p>AE2 UEL 0.56.7 can snapshot the result slot before it performs the later
 * CraftingManager lookup. Returning the selected IRecipe from CraftingManager
 * is therefore not enough on its own: the request stack may already contain
 * the first matching recipe's output. This helper rebuilds the terminal's real
 * 3x3 grid from marked AE2 slots and writes the selected recipe result into the
 * actual result slot immediately before execution.</p>
 */
public final class Ae2TerminalRecipePin {

    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");
    private static final Container MIRROR_OWNER = new MirrorContainer();
    private static final Set<String> LOGGED_PINS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private Ae2TerminalRecipePin() {
    }

    /**
     * Strong execution-time pin used by native AE2's SlotCraftingTerm.doClick.
     */
    public static boolean pinForCraftClick(EntityPlayer player, Slot resultSlot) {
        if (player == null || resultSlot == null || player.openContainer == null) {
            return false;
        }
        Container container = player.openContainer;
        ResourceLocation selectedId = selectedId(container);
        if (selectedId == null) {
            return false;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selectedId);
        if (recipe == null) {
            return false;
        }

        InventoryCrafting matrix = createMirror(container);
        if (matrix == null || !RecipeProbe.matches(recipe, matrix, player.world)) {
            return false;
        }
        return pinResult(container, resultSlot, recipe, matrix, "doClick");
    }

    /**
     * Re-pins a terminal after one of its own matrix refreshes. This path does
     * not need the World because the selection was already validated by the
     * selector packet; it is intentionally limited to containers with the AE2
     * 9+1 marked slot topology.
     */
    public static boolean pinContainerResult(Container container, String phase) {
        if (container == null) {
            return false;
        }
        ResourceLocation selectedId = selectedId(container);
        if (selectedId == null) {
            return false;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selectedId);
        if (recipe == null) {
            return false;
        }
        InventoryCrafting matrix = createMirror(container);
        Slot result = findResultSlot(container);
        if (matrix == null || result == null || isEmpty(matrix)) {
            return false;
        }
        return pinResult(container, result, recipe, matrix, phase);
    }

    /**
     * When CraftingManager is already resolving a selected recipe for a real
     * terminal-owned matrix, keep its live result slot synchronized as well.
     * This is particularly useful for WCT, whose packet path can recalculate
     * the result immediately before consuming it.
     */
    public static void pinOwnerResult(InventoryCrafting matrix, IRecipe recipe) {
        if (matrix == null || recipe == null) {
            return;
        }
        if (!(matrix instanceof dev.sosea1.retropolymorph.core.CraftingMatrixExtension)) {
            return;
        }
        dev.sosea1.retropolymorph.core.CraftingMatrixExtension extension =
                (dev.sosea1.retropolymorph.core.CraftingMatrixExtension) matrix;
        Container owner = extension.retropolymorph$getCraftingOwner();
        if (owner == null || selectedId(owner) == null) {
            return;
        }
        Slot result = findResultSlot(owner);
        if (result == null) {
            return;
        }
        pinResult(owner, result, recipe, matrix, "CraftingManager");
    }

    @Nullable
    public static InventoryCrafting createMirror(Container container) {
        Slot[] inputs = findInputs(container);
        if (inputs == null) {
            return null;
        }
        InventoryCrafting mirror = new InventoryCrafting(MIRROR_OWNER, 3, 3);
        for (int i = 0; i < inputs.length; i++) {
            ItemStack stack = inputs[i].getStack();
            mirror.setInventorySlotContents(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return mirror;
    }

    @Nullable
    private static ResourceLocation selectedId(Container container) {
        ResourceLocation selected = Ae2SelectionStore.get(container);
        if (selected == null && container instanceof Ae2CraftingTermExtension) {
            selected = ((Ae2CraftingTermExtension) container).retropolymorph$getAe2SelectedRecipeId();
            if (selected != null) {
                Ae2SelectionStore.set(container, selected);
            }
        }
        return selected;
    }

    private static boolean pinResult(
            Container container,
            Slot resultSlot,
            IRecipe recipe,
            InventoryCrafting matrix,
            String phase) {
        ItemStack output = RecipeProbe.craftingResult(recipe, matrix);
        if (output.isEmpty()) {
            return false;
        }

        ItemStack before = resultSlot.getStack();
        if (!ItemStack.areItemStacksEqual(before, output)) {
            resultSlot.putStack(output.copy());
        }

        // Capture exactly what was visible/pinned when an execution scope is
        // active. A few wireless/universal terminals later return a stale
        // stack even though their recipe lookup and visible slot are already
        // correct; the SlotCraftingTerm return guard uses this snapshot.
        if (Ae2CraftExecutionScope.isActive()
                && Ae2CraftExecutionScope.currentContainer() == container
                && ("doClick".equals(phase) || "addonDoAction".equals(phase))) {
            Ae2CraftExecutionScope.setExpectedOutput(output);
        }

        ResourceLocation recipeId = recipe.getRegistryName();
        String key = container.getClass().getName() + "|" + phase + "|" + String.valueOf(recipeId);
        if (LOGGED_PINS.add(key)) {
            LOGGER.debug(
                    "AE2 selected result pin active: phase={}, container={}, selected={}, before={}, after={}",
                    phase,
                    container.getClass().getName(),
                    recipeId,
                    stackName(before),
                    stackName(output));
        }
        return true;
    }

    @Nullable
    private static Slot[] findInputs(Container container) {
        List<Slot> slots = new ArrayList<Slot>(9);
        for (Slot slot : container.inventorySlots) {
            if (slot instanceof Ae2CraftingMatrixSlot) {
                slots.add(slot);
            }
        }
        if (slots.size() != 9) {
            return null;
        }
        Collections.sort(slots, new Comparator<Slot>() {
            @Override
            public int compare(Slot left, Slot right) {
                int byY = Integer.compare(left.yPos, right.yPos);
                return byY != 0 ? byY : Integer.compare(left.xPos, right.xPos);
            }
        });
        return slots.toArray(new Slot[9]);
    }

    @Nullable
    private static Slot findResultSlot(Container container) {
        Slot result = null;
        for (Slot slot : container.inventorySlots) {
            if (!(slot instanceof Ae2CraftingResultSlot)) {
                continue;
            }
            if (result != null) {
                return null;
            }
            result = slot;
        }
        return result;
    }

    private static boolean isEmpty(InventoryCrafting matrix) {
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            if (!matrix.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String stackName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        return String.valueOf(id) + "x" + stack.getCount() + ":" + stack.getMetadata();
    }

    private static final class MirrorContainer extends Container {
        @Override
        public void onCraftMatrixChanged(IInventory inventory) {
            // Scratch copy only; never propagate container events.
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }
}
