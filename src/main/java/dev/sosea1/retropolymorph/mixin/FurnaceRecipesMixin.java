package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.furnace.FurnaceConflictExtension;
import dev.sosea1.retropolymorph.furnace.FurnaceConflictRecipe;
import dev.sosea1.retropolymorph.furnace.FurnaceRecipeResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Preserves only registrations that FurnaceRecipes is about to reject because
 * another recipe already matches the same input. The vanilla map remains
 * untouched, so default behavior is unchanged until a player selects an option.
 */
@Mixin(FurnaceRecipes.class)
public abstract class FurnaceRecipesMixin implements FurnaceConflictExtension {

    @Unique
    private List<FurnaceConflictRecipe> retropolymorph$rejectedConflicts;

    @Unique
    private List<FurnaceConflictRecipe> retropolymorph$rejectedConflictsView;

    @Override
    public List<FurnaceConflictRecipe> retropolymorph$getRejectedConflicts() {
        List<FurnaceConflictRecipe> view = this.retropolymorph$rejectedConflictsView;
        return view == null ? Collections.<FurnaceConflictRecipe>emptyList() : view;
    }

    @Inject(method = "addSmeltingRecipe", at = @At("RETURN"))
    private void retropolymorph$captureRejectedConflict(
            ItemStack input,
            ItemStack output,
            float experience,
            CallbackInfo ci) {
        FurnaceRecipes recipes = (FurnaceRecipes) (Object) this;
        if (!FurnaceRecipeResolver.registrationWasRejected(recipes, input, output)) {
            return;
        }

        List<FurnaceConflictRecipe> conflicts = retropolymorph$getOrCreateRejectedConflicts();
        for (FurnaceConflictRecipe conflict : conflicts) {
            if (conflict.sameRegistration(input, output, experience)) {
                return;
            }
        }
        conflicts.add(new FurnaceConflictRecipe(input, output, experience));
    }

    @Unique
    private List<FurnaceConflictRecipe> retropolymorph$getOrCreateRejectedConflicts() {
        List<FurnaceConflictRecipe> conflicts = this.retropolymorph$rejectedConflicts;
        if (conflicts == null) {
            conflicts = new ArrayList<FurnaceConflictRecipe>();
            this.retropolymorph$rejectedConflicts = conflicts;
            this.retropolymorph$rejectedConflictsView = Collections.unmodifiableList(conflicts);
        }
        return conflicts;
    }
}
