package com.sosea1.retropolymorph.compat.rftools;

import com.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/** Pending editor choice; the native Apply packet commits it to a saved template. */
public final class RFToolsPreviewSelection {
    private static final Map<IInventory, Pending> SELECTIONS = new WeakHashMap<>();

    private RFToolsPreviewSelection() { }

    public static synchronized void set(IInventory inventory, @Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            SELECTIONS.remove(inventory);
        } else {
            SELECTIONS.put(inventory, new Pending(inventory, recipeId));
        }
    }

    @Nullable
    public static synchronized ResourceLocation get(IInventory inventory) {
        Pending pending = SELECTIONS.get(inventory);
        if (pending == null) { return null; }
        for (int i = 0; i < 9; i++) {
            if (!ItemStack.areItemStacksEqual(pending.inputs[i], inventory.getStackInSlot(i))) {
                SELECTIONS.remove(inventory);
                return null;
            }
        }
        return pending.recipeId;
    }

    @Nullable
    public static IRecipe resolve(@Nullable ResourceLocation id, InventoryCrafting matrix, World world) {
        IRecipe recipe = id == null ? null : ForgeRegistries.RECIPES.getValue(id);
        return recipe != null && RecipeProbe.matches(recipe, matrix, world) ? recipe : null;
    }

    private static final class Pending {
        private final ResourceLocation recipeId;
        private final ItemStack[] inputs = new ItemStack[9];

        private Pending(IInventory inventory, ResourceLocation recipeId) {
            this.recipeId = recipeId;
            for (int i = 0; i < 9; i++) { this.inputs[i] = inventory.getStackInSlot(i).copy(); }
        }
    }
}
