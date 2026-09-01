package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2CraftingTermExtension;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Pseudo
@Mixin(targets = {
        "appeng.container.implementations.ContainerCraftingTerm",
        "p455w0rd.wct.container.ContainerWCT"
}, remap = false)
public abstract class Ae2CraftingTermMixin implements Ae2CraftingTermExtension {

    @Shadow(remap = false)
    @Nullable
    private IRecipe currentRecipe;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$selectedRecipeId;

    @Override
    @Nullable
    public IRecipe retropolymorph$getAe2CurrentRecipe() {
        return this.currentRecipe;
    }

    @Override
    public void retropolymorph$setAe2CurrentRecipe(@Nullable IRecipe recipe) {
        this.currentRecipe = recipe;
    }

    @Override
    @Nullable
    public ResourceLocation retropolymorph$getAe2SelectedRecipeId() {
        return this.retropolymorph$selectedRecipeId;
    }

    @Inject(method = "func_75130_a", at = @At("HEAD"), remap = false)
    private void retropolymorph$onMatrixChangedHead(IInventory inv, CallbackInfo ci) {
        if (this.retropolymorph$selectedRecipeId != null) {
            IRecipe selected = ForgeRegistries.RECIPES.getValue(this.retropolymorph$selectedRecipeId);
            if (selected != null) {
                this.currentRecipe = selected;
            }
        }
    }

    @Override
    public void retropolymorph$setAe2SelectedRecipeId(@Nullable ResourceLocation recipeId) {
        this.retropolymorph$selectedRecipeId = recipeId;
    }
}
