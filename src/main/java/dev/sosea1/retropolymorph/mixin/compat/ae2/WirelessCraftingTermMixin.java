package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2CraftingTermExtension;
import dev.sosea1.retropolymorph.compat.ae2.Ae2SelectionStore;
import dev.sosea1.retropolymorph.compat.ae2.Ae2TerminalRecipePin;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** AE2WTLib-family selection bridge. */
@Pseudo
@Mixin(targets = "p455w0rd.ae2wtlib.api.container.ContainerWT", remap = false)
public abstract class WirelessCraftingTermMixin implements Ae2CraftingTermExtension {

    @Unique
    @Nullable
    private IRecipe retropolymorph$wirelessCurrentRecipe;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$wirelessSelectedRecipeId;

    @Override
    @Nullable
    public IRecipe retropolymorph$getAe2CurrentRecipe() {
        return this.retropolymorph$wirelessCurrentRecipe;
    }

    @Override
    public void retropolymorph$setAe2CurrentRecipe(@Nullable IRecipe recipe) {
        this.retropolymorph$wirelessCurrentRecipe = recipe;
    }

    @Override
    @Nullable
    public ResourceLocation retropolymorph$getAe2SelectedRecipeId() {
        ResourceLocation stored = Ae2SelectionStore.get((Container) (Object) this);
        return stored != null ? stored : this.retropolymorph$wirelessSelectedRecipeId;
    }

    @Override
    public void retropolymorph$setAe2SelectedRecipeId(@Nullable ResourceLocation recipeId) {
        this.retropolymorph$wirelessSelectedRecipeId = recipeId;
        Ae2SelectionStore.set((Container) (Object) this, recipeId);
    }

    @Inject(method = "func_75130_a", at = @At("HEAD"), remap = false, require = 0)
    private void retropolymorph$seedWirelessMatrix(IInventory inventory, CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationAe2Enabled()) {
            return;
        }
        Container self = (Container) (Object) this;
        dev.sosea1.retropolymorph.compat.ae2.Ae2MatrixChangeScope.enter(self);
        Ae2TerminalRecipePin.handleMatrixChangedHead(self);
    }

    @Inject(method = "func_75130_a", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$pinWirelessResult(IInventory inventory, CallbackInfo ci) {
        try {
            if (!PolymorphConfig.isIntegrationAe2Enabled()) {
                return;
            }
            Ae2TerminalRecipePin.handleMatrixChangedReturn((Container) (Object) this, "wirelessBaseRefresh");
        } finally {
            dev.sosea1.retropolymorph.compat.ae2.Ae2MatrixChangeScope.exit();
        }
    }

}
