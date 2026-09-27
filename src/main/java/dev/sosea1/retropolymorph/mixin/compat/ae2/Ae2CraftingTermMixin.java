package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2CraftingTermExtension;
import dev.sosea1.retropolymorph.compat.ae2.Ae2MatrixChangeScope;
import dev.sosea1.retropolymorph.compat.ae2.Ae2SelectionStore;
import dev.sosea1.retropolymorph.compat.ae2.Ae2TerminalRecipePin;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
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
        "appeng.container.implementations.ContainerWirelessCraftingTerminal",
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
        ResourceLocation stored = Ae2SelectionStore.get((Container) (Object) this);
        return stored != null ? stored : this.retropolymorph$selectedRecipeId;
    }

    @Override
    public void retropolymorph$setAe2SelectedRecipeId(@Nullable ResourceLocation recipeId) {
        this.retropolymorph$selectedRecipeId = recipeId;
        Ae2SelectionStore.set((Container) (Object) this, recipeId);
    }

    @Inject(method = "func_75130_a", at = @At("HEAD"), remap = false, require = 0)
    private void retropolymorph$onMatrixChangedHead(IInventory inv, CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationAe2Enabled()) {
            return;
        }
        Container self = (Container) (Object) this;
        Ae2MatrixChangeScope.enter(self);
        Ae2TerminalRecipePin.handleMatrixChangedHead(self);
    }

    @Inject(method = "func_75130_a", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$onMatrixChangedReturn(IInventory inv, CallbackInfo ci) {
        try {
            if (!PolymorphConfig.isIntegrationAe2Enabled()) {
                return;
            }
            Ae2TerminalRecipePin.handleMatrixChangedReturn((Container) (Object) this, "craftingTermRefresh");
        } finally {
            Ae2MatrixChangeScope.exit();
        }
    }
}
