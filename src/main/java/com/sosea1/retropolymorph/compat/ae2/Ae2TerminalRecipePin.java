package com.sosea1.retropolymorph.compat.ae2;

import com.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
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
import java.lang.reflect.Method;
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

    private static final java.lang.reflect.Field LISTENERS_FIELD;
    static {
        java.lang.reflect.Field field = null;
        try {
            field = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                    Container.class, "listeners", "field_75149_d");
            field.setAccessible(true);
        } catch (Throwable ignored) {
        }
        LISTENERS_FIELD = field;
    }

    /**
     * Resolves the player from listeners, then AE2's inventory bridge and finally
     * ordinary slots. AppEngSlot uses emptyInventory, so slot scanning alone is insufficient.
     */
    @Nullable
    public static EntityPlayer resolvePlayer(Container container) {
        if (container == null) {
            return null;
        }
        // 1. Check container.listeners (on server side, viewing player is always in listeners)
        if (LISTENERS_FIELD != null) {
            try {
                Object list = LISTENERS_FIELD.get(container);
                if (list instanceof List) {
                    for (Object listener : (List<?>) list) {
                        if (listener instanceof EntityPlayer) {
                            return (EntityPlayer) listener;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        // 2. getPlayerInv(), getInventoryPlayer(), or getPlayer() traversing class hierarchy (AEBaseContainer)
        Class<?> clazz = container.getClass();
        while (clazz != null && clazz != Object.class && clazz != Container.class) {
            for (String methodName : new String[]{"getPlayerInv", "getInventoryPlayer", "getPlayer"}) {
                try {
                    Method getter = clazz.getDeclaredMethod(methodName);
                    getter.setAccessible(true);
                    Object res = getter.invoke(container);
                    if (res instanceof InventoryPlayer) {
                        return ((InventoryPlayer) res).player;
                    } else if (res instanceof EntityPlayer) {
                        return (EntityPlayer) res;
                    }
                } catch (Throwable ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        // 3. Generic slot fallback
        for (Slot slot : container.inventorySlots) {
            if (slot != null && slot.inventory instanceof InventoryPlayer) {
                return ((InventoryPlayer) slot.inventory).player;
            }
        }
        // 4. Client-side fallback if container is currently open
        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
            EntityPlayer clientPlayer = ClientResolver.getClientPlayer(container);
            if (clientPlayer != null) {
                return clientPlayer;
            }
        }
        return null;
    }

    private static final class ClientResolver {
        @Nullable
        static EntityPlayer getClientPlayer(Container container) {
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                if (mc != null && mc.player != null) {
                    if (container == null
                            || mc.player.openContainer == container
                            || mc.player.inventoryContainer == container
                            || mc.currentScreen != null) {
                        return mc.player;
                    }
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    /**
     * Resolves the world for this container from its viewing player.
     */
    @Nullable
    public static World resolveWorld(Container container) {
        EntityPlayer player = resolvePlayer(container);
        if (player != null && player.world != null) {
            return player.world;
        }
        return null;
    }

    /**
     * Clears all selection state associated with the container.
     */
    public static void clearSelection(Container container) {
        if (container == null) {
            return;
        }
        Ae2SelectionStore.set(container, null);
        if (container instanceof Ae2CraftingTermExtension) {
            ((Ae2CraftingTermExtension) container).retropolymorph$setAe2SelectedRecipeId(null);
        }
        clearNativeMatrices(container);
    }

    public static void seedNativeMatrices(Container container, ResourceLocation recipeId) {
        if (container == null || recipeId == null) {
            return;
        }
        InventoryCrafting last = null;
        for (Slot slot : container.inventorySlots) {
            if (slot instanceof Ae2CraftingMatrixSlot && slot.inventory instanceof InventoryCrafting) {
                InventoryCrafting nativeMatrix = (InventoryCrafting) slot.inventory;
                if (nativeMatrix != last) {
                    com.sosea1.retropolymorph.core.RecipeSelectionSeeder.seed(nativeMatrix, recipeId);
                    last = nativeMatrix;
                }
            }
        }
    }

    public static void clearNativeMatrices(Container container) {
        if (container == null) {
            return;
        }
        InventoryCrafting last = null;
        for (Slot slot : container.inventorySlots) {
            if (slot instanceof Ae2CraftingMatrixSlot && slot.inventory instanceof InventoryCrafting) {
                InventoryCrafting nativeMatrix = (InventoryCrafting) slot.inventory;
                if (nativeMatrix != last) {
                    com.sosea1.retropolymorph.core.RecipeSelectionSeeder.clear(nativeMatrix);
                    last = nativeMatrix;
                }
            }
        }
    }

    /**
     * Synchronous matrix change hook executed at onCraftMatrixChanged HEAD.
     *
     * <p>Revalidates current selection, clearing any stale selection that no longer
     * matches the current matrix, and synchronously preseeds the player's saved
     * preference into AE2's {@code currentRecipe} so AE2 never falls back to the
     * default first recipe.</p>
     */
    public static void handleMatrixChangedHead(Container container) {
        if (container == null) {
            return;
        }

        InventoryCrafting matrix = createMirror(container);
        if (matrix == null) {
            return;
        }

        if (isEmpty(matrix)) {
            clearSelection(container);
            if (container instanceof Ae2CraftingTermExtension) {
                ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(null);
            }
            return;
        }

        World world = resolveWorld(container);
        ResourceLocation selectedId = selectedId(container);
        IRecipe selectedRecipe = selectedId == null ? null : ForgeRegistries.RECIPES.getValue(selectedId);

        if (selectedRecipe != null && world != null && RecipeProbe.matches(selectedRecipe, matrix, world)) {
            if (container instanceof Ae2CraftingTermExtension) {
                ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(selectedRecipe);
            }
            seedNativeMatrices(container, selectedId);
            return;
        }

        // Previous selection is no longer valid for this matrix
        clearSelection(container);
        if (container instanceof Ae2CraftingTermExtension) {
            ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(null);
        }

        EntityPlayer player = resolvePlayer(container);
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            com.sosea1.retropolymorph.core.CraftingPreferenceSeeder.preseed(
                    (net.minecraft.entity.player.EntityPlayerMP) player, container);
            ResourceLocation preseededId = selectedId(container);
            if (preseededId != null) {
                IRecipe preseededRecipe = ForgeRegistries.RECIPES.getValue(preseededId);
                if (preseededRecipe != null && world != null && RecipeProbe.matches(preseededRecipe, matrix, world)) {
                    if (container instanceof Ae2CraftingTermExtension) {
                        ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(preseededRecipe);
                    }
                    seedNativeMatrices(container, preseededId);
                } else {
                    clearSelection(container);
                }
            }
        }
    }

    /**
     * Synchronous matrix change hook executed at onCraftMatrixChanged RETURN.
     *
     * <p>Validates that the selected recipe actually matches the current matrix
     * before pinning the result slot. If the recipe no longer matches or the
     * inputs were broken, any ghost result is immediately cleared.</p>
     */
    public static boolean handleMatrixChangedReturn(Container container, String phase) {
        if (container == null) {
            return false;
        }

        InventoryCrafting matrix = createMirror(container);
        Slot result = findResultSlot(container);
        if (matrix == null || result == null) {
            return false;
        }

        if (isEmpty(matrix)) {
            clearSelection(container);
            if (container instanceof Ae2CraftingTermExtension) {
                ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(null);
            }
            if (!result.getStack().isEmpty()) {
                result.putStack(ItemStack.EMPTY);
            }
            return false;
        }

        ResourceLocation selectedId = selectedId(container);
        if (selectedId == null) {
            if (container instanceof Ae2CraftingTermExtension) {
                IRecipe current = ((Ae2CraftingTermExtension) container).retropolymorph$getAe2CurrentRecipe();
                // If currentRecipe is still set without a selection, validate it and clear the
                // ghost slot only if world is known and recipe no longer matches.
                if (current != null) {
                    World world = resolveWorld(container);
                    if (world != null && !RecipeProbe.matches(current, matrix, world)) {
                        ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(null);
                        if (!result.getStack().isEmpty()) {
                            result.putStack(ItemStack.EMPTY);
                        }
                    }
                }
            }
            return false;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selectedId);
        World world = resolveWorld(container);

        if (recipe == null || !RecipeProbe.matches(recipe, matrix, world)) {
            clearSelection(container);
            if (container instanceof Ae2CraftingTermExtension) {
                ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(null);
            }
            if (!result.getStack().isEmpty()) {
                result.putStack(ItemStack.EMPTY);
            }
            return false;
        }

        if (container instanceof Ae2CraftingTermExtension) {
            ((Ae2CraftingTermExtension) container).retropolymorph$setAe2CurrentRecipe(recipe);
        }
        return pinResult(container, result, recipe, matrix, phase);
    }

    /**
     * Re-pins a terminal after one of its own matrix refreshes.
     */
    public static boolean pinContainerResult(Container container, String phase) {
        return handleMatrixChangedReturn(container, phase);
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
        if (!(matrix instanceof com.sosea1.retropolymorph.core.CraftingMatrixExtension)) {
            return;
        }
        com.sosea1.retropolymorph.core.CraftingMatrixExtension extension =
                (com.sosea1.retropolymorph.core.CraftingMatrixExtension) matrix;
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
