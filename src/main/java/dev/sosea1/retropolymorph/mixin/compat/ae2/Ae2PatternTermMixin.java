package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2PatternTermExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Soft Pattern Terminal bridge.
 *
 * AE2 creates temporary InventoryCrafting instances for preview and direct
 * pattern crafting. The selected recipe is seeded into only those matrices;
 * the normal Retro Polymorph CraftingManager hook performs final validation.
 */
@Pseudo
@Mixin(targets = "appeng.container.implementations.ContainerPatternTerm", remap = false)
public abstract class Ae2PatternTermMixin implements Ae2PatternTermExtension {

    @Shadow(remap = false)
    public boolean craftingMode;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$patternSelectedRecipeId;

    @Unique
    private boolean retropolymorph$patternPreviewObserved;

    @Unique
    @Nullable
    private InventoryCrafting retropolymorph$patternPreviewMatrix;

    @Invoker(value = "getAndUpdateOutput", remap = false)
    @Override
    public abstract ItemStack retropolymorph$refreshPatternOutput();

    @Override
    public boolean retropolymorph$isPatternCraftingMode() {
        return this.craftingMode;
    }

    @Override
    @Nullable
    public ResourceLocation retropolymorph$getPatternSelectedRecipeId() {
        return this.retropolymorph$patternSelectedRecipeId;
    }

    @Override
    public void retropolymorph$setPatternSelectedRecipeId(@Nullable ResourceLocation recipeId) {
        this.retropolymorph$patternSelectedRecipeId = recipeId;
    }

    @Override
    public void retropolymorph$resetPatternPreviewObservation() {
        this.retropolymorph$patternPreviewObserved = false;
    }

    @Override
    public boolean retropolymorph$wasPatternPreviewObserved() {
        return this.retropolymorph$patternPreviewObserved;
    }

    @ModifyVariable(
            method = "getAndUpdateOutput",
            at = @At("STORE"),
            ordinal = 0,
            remap = false,
            require = 1)
    private InventoryCrafting retropolymorph$seedPatternPreview(InventoryCrafting matrix) {
        ResourceLocation selected = this.retropolymorph$patternSelectedRecipeId;
        this.retropolymorph$patternPreviewMatrix = matrix;
        RecipeSelectionSeeder.seed(matrix, selected);
        return matrix;
    }

    @Inject(
            method = "getAndUpdateOutput",
            at = @At("RETURN"),
            remap = false,
            require = 1)
    private void retropolymorph$finishPatternPreview(CallbackInfoReturnable<ItemStack> cir) {
        ResourceLocation selected = this.retropolymorph$patternSelectedRecipeId;
        InventoryCrafting matrix = this.retropolymorph$patternPreviewMatrix;
        boolean observed = matrix != null
                && RecipeSelectionSeeder.wasOutputResolutionObserved(matrix, selected);

        this.retropolymorph$patternPreviewObserved = observed;
        this.retropolymorph$patternPreviewMatrix = null;

        if (selected != null && !observed) {
            this.retropolymorph$patternSelectedRecipeId = null;
        }
    }

    @ModifyVariable(
            method = "craftOrGetItem",
            at = @At("STORE"),
            ordinal = 0,
            remap = false,
            require = 1)
    private InventoryCrafting retropolymorph$seedPatternRequest(InventoryCrafting matrix) {
        RecipeSelectionSeeder.seed(matrix, this.retropolymorph$patternSelectedRecipeId);
        return matrix;
    }

    @ModifyVariable(
            method = "craftOrGetItem",
            at = @At("STORE"),
            ordinal = 1,
            remap = false,
            require = 1)
    private InventoryCrafting retropolymorph$seedPatternRealInputs(InventoryCrafting matrix) {
        RecipeSelectionSeeder.seed(matrix, this.retropolymorph$patternSelectedRecipeId);
        return matrix;
    }
}
