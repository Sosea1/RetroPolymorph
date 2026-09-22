package dev.sosea1.retropolymorph.compat.extendedcrafting;

import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Machine surface implementation for Extended Crafting's Ender Crafter ("Эндер-верстак"). */
final class ExtendedEnderCrafterSurface implements MachineRecipeSurface {

    private static final String KEY_PREFIX = "extendedcrafting:ender/";

    private static final Container MIRROR_OWNER = new Container() {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    };

    private final Container container;
    private final TileEntity tile;
    private final EnderCrafterSelectionAccess selection;
    private final InventoryCrafting matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);

    ExtendedEnderCrafterSurface(Container container, TileEntity tile) {
        this.container = container;
        this.tile = tile;
        this.selection = tile instanceof EnderCrafterSelectionAccess ? (EnderCrafterSelectionAccess) tile : null;
        refreshMatrix();
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public Object getRecipeOwner() {
        return this.tile;
    }

    @Override
    public int getInputCount() {
        return 9;
    }

    @Override
    public ItemStack getInputStack(int index) {
        if (index < 0 || index >= 9) {
            return ItemStack.EMPTY;
        }
        int slotIndex = 1 + index;
        if (slotIndex >= this.container.inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        return this.container.inventorySlots.get(slotIndex).getStack();
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        if (world == null || isGridEmpty()) {
            return Collections.emptyList();
        }
        refreshMatrix();
        List<IRecipe> enderRecipes = getEnderRecipes();
        if (enderRecipes.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecipeOption> options = new ArrayList<RecipeOption>();
        for (int i = 0; i < enderRecipes.size(); i++) {
            IRecipe recipe = enderRecipes.get(i);
            if (recipe != null && RecipeProbe.matches(recipe, this.matrix, world)) {
                String key = KEY_PREFIX + i;
                ItemStack output = RecipeProbe.craftingResult(recipe, this.matrix);
                if (!output.isEmpty()) {
                    options.add(new RecipeOption(key, output));
                }
            }
        }
        return options.size() > 1 ? options : Collections.<RecipeOption>emptyList();
    }

    @Override
    public boolean selectRecipe(String recipeKey, World world) {
        int index = parseIndex(recipeKey);
        if (index < 0 || world == null || this.selection == null) {
            return false;
        }
        List<IRecipe> enderRecipes = getEnderRecipes();
        if (index >= enderRecipes.size()) {
            return false;
        }
        refreshMatrix();
        IRecipe recipe = enderRecipes.get(index);
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }
        ItemStack output = EnderCrafterRecipeIdentity.resolveEffectiveOutput(recipe, this.matrix);
        this.selection.retropolymorph$setEnderCrafterRecipe(index, output);
        this.tile.markDirty();
        return true;
    }

    @Override
    public void clearRecipeSelection() {
        if (this.selection != null) {
            this.selection.retropolymorph$setEnderCrafterRecipe(-1, null);
            this.tile.markDirty();
        }
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        if (this.selection == null) {
            return null;
        }
        int index = this.selection.retropolymorph$getEnderCrafterRecipeIndex();
        return index < 0 ? null : KEY_PREFIX + index;
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        if (this.selection == null) {
            return;
        }
        int index = parseIndex(recipeKey);
        ItemStack output = null;
        if (index >= 0) {
            refreshMatrix();
            List<IRecipe> enderRecipes = getEnderRecipes();
            if (index < enderRecipes.size()) {
                IRecipe recipe = enderRecipes.get(index);
                if (recipe != null) {
                    output = EnderCrafterRecipeIdentity.resolveEffectiveOutput(recipe, this.matrix);
                }
            }
        }
        this.selection.retropolymorph$setEnderCrafterRecipe(index, output);
    }

    @Override
    public MachineRecipePersistence getPersistencePolicy() {
        return MachineRecipePersistence.OWNER;
    }

    @Override
    public boolean controlsActualOperation() {
        return true;
    }

    @Override
    @Nullable
    public Slot getResultSlot() {
        return this.container.inventorySlots.isEmpty() ? null : this.container.inventorySlots.get(0);
    }

    @Override
    public SelectorPlacement getSelectorPlacement() {
        Slot result = getResultSlot();
        int x = result != null ? result.xPos : 124;
        int y = result != null ? result.yPos : 36;
        return SelectorPlacement.resultSlot(x, y, 0, 0);
    }

    private void refreshMatrix() {
        for (int i = 0; i < 9; i++) {
            this.matrix.setInventorySlotContents(i, getInputStack(i));
        }
    }

    private boolean isGridEmpty() {
        for (int i = 0; i < 9; i++) {
            if (!getInputStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int parseIndex(@Nullable String recipeKey) {
        if (recipeKey == null || !recipeKey.startsWith(KEY_PREFIX)) {
            return -1;
        }
        try {
            return Integer.parseInt(recipeKey.substring(KEY_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<IRecipe> getEnderRecipes() {
        try {
            Class<?> managerClass = Class.forName("com.blakebr0.extendedcrafting.crafting.endercrafter.EnderCrafterRecipeManager");
            Method getInstance = managerClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method getRecipes = managerClass.getMethod("getRecipes");
            Object result = getRecipes.invoke(instance);
            if (result instanceof List) {
                return (List<IRecipe>) result;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                    "extendedcrafting",
                    "getEnderRecipes",
                    error);
        }
        return Collections.emptyList();
    }
}
