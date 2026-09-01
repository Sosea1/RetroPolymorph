package dev.sosea1.retropolymorph.mixin.compat.extendedcrafting;

import dev.sosea1.retropolymorph.compat.extendedcrafting.ExtendedTableMatrixExtension;
import net.minecraft.item.crafting.IRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

/** Soft selected-recipe state for Extended Crafting's TableCrafting matrix. */
@Pseudo
@Mixin(targets = "com.blakebr0.extendedcrafting.crafting.table.TableCrafting", remap = false)
public abstract class ExtendedTableCraftingMixin implements ExtendedTableMatrixExtension {

    @Unique
    private int retropolymorph$selectedTableRecipeIndex = -1;

    @Unique
    @Nullable
    private IRecipe retropolymorph$selectedTableRecipe;

    @Unique
    private boolean retropolymorph$tableResolutionObserved;

    @Override
    public int retropolymorph$getSelectedTableRecipeIndex() {
        return this.retropolymorph$selectedTableRecipeIndex;
    }

    @Override
    @Nullable
    public IRecipe retropolymorph$getSelectedTableRecipe() {
        return this.retropolymorph$selectedTableRecipe;
    }

    @Override
    public void retropolymorph$selectTableRecipe(int index, IRecipe recipe) {
        this.retropolymorph$selectedTableRecipeIndex = index;
        this.retropolymorph$selectedTableRecipe = recipe;
        this.retropolymorph$tableResolutionObserved = false;
    }

    @Override
    public void retropolymorph$clearTableRecipe() {
        this.retropolymorph$selectedTableRecipeIndex = -1;
        this.retropolymorph$selectedTableRecipe = null;
        this.retropolymorph$tableResolutionObserved = false;
    }

    @Override
    public void retropolymorph$markTableResolutionObserved() {
        this.retropolymorph$tableResolutionObserved = true;
    }

    @Override
    public boolean retropolymorph$wasTableResolutionObserved() {
        return this.retropolymorph$tableResolutionObserved;
    }
}
