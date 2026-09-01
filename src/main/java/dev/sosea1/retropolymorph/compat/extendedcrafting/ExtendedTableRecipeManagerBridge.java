package dev.sosea1.retropolymorph.compat.extendedcrafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import java.util.List;

/** Soft bridge into Extended Crafting's table recipe manager. */
public interface ExtendedTableRecipeManagerBridge {

    List<IRecipe> retropolymorph$getTableRecipes();

    /**
     * Executes Extended Crafting's normal output resolver. This is used only to
     * detect whether its optional 3x3 Forge-recipe fallback is actually active;
     * no config class or reflection is required.
     */
    ItemStack retropolymorph$findTableResult(InventoryCrafting matrix, World world);
}
