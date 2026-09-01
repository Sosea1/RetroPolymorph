package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(InventoryCrafting.class)
public abstract class InventoryCraftingMixin implements CraftingMatrixExtension {

    @Unique
    @Nullable
    private RecipeSelectionState retropolymorph$recipeSelectionState;

    @Override
    @Nullable
    public RecipeSelectionState retropolymorph$peekRecipeSelectionState() {
        return this.retropolymorph$recipeSelectionState;
    }

    @Override
    public RecipeSelectionState retropolymorph$getOrCreateRecipeSelectionState() {
        RecipeSelectionState state = this.retropolymorph$recipeSelectionState;
        if (state == null) {
            state = new RecipeSelectionState();
            this.retropolymorph$recipeSelectionState = state;
        }
        return state;
    }
}
