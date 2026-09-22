package dev.sosea1.retropolymorph.mixin;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.furnace.FurnaceRecipeResolver;
import dev.sosea1.retropolymorph.furnace.FurnaceSelectionExtension;
import dev.sosea1.retropolymorph.furnace.FurnaceSelectionState;
import dev.sosea1.retropolymorph.furnace.FurnaceSelectionStore;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds persistent, per-furnace recipe selection without global state. */
@Mixin(TileEntityFurnace.class)
public abstract class TileEntityFurnaceMixin implements FurnaceSelectionExtension {

    @Unique
    private static final String RETROPOLYMORPH_RECIPE_TAG = "retropolymorphSelectedSmelting";

    @Unique
    private static final String RETROPOLYMORPH_EXPERIENCE_TAG = "retropolymorphSmeltingExperience";

    @Unique
    private static final int OUTPUT_SLOT = 2;

    @Override
    public FurnaceSelectionState retropolymorph$peekFurnaceSelectionState() {
        return FurnaceSelectionStore.peek((TileEntityFurnace) (Object) this);
    }

    @Override
    public FurnaceSelectionState retropolymorph$getOrCreateFurnaceSelectionState() {
        return FurnaceSelectionStore.getOrCreate((TileEntityFurnace) (Object) this);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void retropolymorph$readSelection(NBTTagCompound compound, CallbackInfo ci) {
        String recipeKey = compound.getString(RETROPOLYMORPH_RECIPE_TAG);
        ItemStack output = ((TileEntityFurnace) (Object) this).getStackInSlot(OUTPUT_SLOT);
        boolean hasRecipe = RecipeKey.isWireSafe(recipeKey);
        boolean hasExperience = !output.isEmpty() && compound.hasKey(RETROPOLYMORPH_EXPERIENCE_TAG);

        FurnaceSelectionState state = retropolymorph$peekFurnaceSelectionState();
        if (hasRecipe || hasExperience) {
            state = retropolymorph$getOrCreateFurnaceSelectionState();
        }
        if (state == null) {
            return;
        }

        if (hasRecipe) {
            state.setPersistentKey(recipeKey);
        } else {
            state.clear();
        }

        if (hasExperience) {
            state.restoreTrackedExperience(
                    output, compound.getFloat(RETROPOLYMORPH_EXPERIENCE_TAG));
        } else {
            state.clearTrackedExperience();
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void retropolymorph$writeSelection(
            NBTTagCompound compound,
            CallbackInfoReturnable<NBTTagCompound> cir) {
        NBTTagCompound result = cir.getReturnValue();
        FurnaceSelectionState state = retropolymorph$peekFurnaceSelectionState();
        String recipeKey = state == null ? null : state.getSelectedKey();
        if (recipeKey == null) {
            result.removeTag(RETROPOLYMORPH_RECIPE_TAG);
        } else {
            result.setString(RETROPOLYMORPH_RECIPE_TAG, recipeKey);
        }

        ItemStack output = ((TileEntityFurnace) (Object) this).getStackInSlot(OUTPUT_SLOT);
        if (state != null && state.hasTrackedExperience(output)) {
            result.setFloat(RETROPOLYMORPH_EXPERIENCE_TAG, state.getTrackedExperience());
        } else {
            result.removeTag(RETROPOLYMORPH_EXPERIENCE_TAG);
        }
    }

    @Redirect(
            method = "canSmelt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/FurnaceRecipes;"
                            + "getSmeltingResult(Lnet/minecraft/item/ItemStack;)"
                            + "Lnet/minecraft/item/ItemStack;"),
            require = 1)
    private ItemStack retropolymorph$resolveCanSmelt(FurnaceRecipes recipes, ItemStack input) {
        return retropolymorph$resolveSelectedSmelting(recipes, input);
    }

    @Redirect(
            method = "smeltItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/FurnaceRecipes;"
                            + "getSmeltingResult(Lnet/minecraft/item/ItemStack;)"
                            + "Lnet/minecraft/item/ItemStack;"),
            require = 1)
    private ItemStack retropolymorph$resolveSmeltItem(FurnaceRecipes recipes, ItemStack input) {
        ItemStack result = retropolymorph$resolveSelectedSmelting(recipes, input);
        FurnaceSelectionState state = retropolymorph$peekFurnaceSelectionState();
        if (state == null || result.isEmpty()) {
            return result;
        }

        TileEntityFurnace furnace = (TileEntityFurnace) (Object) this;
        ItemStack existingOutput = furnace.getStackInSlot(OUTPUT_SLOT);
        float producedExperience = FurnaceRecipeResolver.getCurrentRecipeExperience(
                recipes, state, result);
        float vanillaExistingExperience = existingOutput.isEmpty()
                ? 0.0F
                : recipes.getSmeltingExperience(existingOutput);
        state.recordProducedOutput(
                existingOutput,
                result,
                producedExperience,
                vanillaExistingExperience);
        return result;
    }

    @Unique
    private ItemStack retropolymorph$resolveSelectedSmelting(
            FurnaceRecipes recipes,
            ItemStack input) {
        FurnaceSelectionState state = retropolymorph$peekFurnaceSelectionState();
        if (state == null) {
            return recipes.getSmeltingResult(input);
        }
        String previousKey = state.getSelectedKey();
        ItemStack result = FurnaceRecipeResolver.resolve(
                recipes,
                input,
                state);
        if (previousKey != null && state.getSelectedKey() == null) {
            ((TileEntityFurnace) (Object) this).markDirty();
        }
        return result;
    }
}
