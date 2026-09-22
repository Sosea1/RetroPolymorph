package dev.sosea1.retropolymorph.mixin.compat.extendedcrafting;

import dev.sosea1.retropolymorph.compat.extendedcrafting.ExtendedCraftingBridgeRegistry;
import dev.sosea1.retropolymorph.compat.extendedcrafting.ExtendedTableRecipeManagerBridge;
import dev.sosea1.retropolymorph.compat.extendedcrafting.ExtendedTableSelectionResolver;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Hooks Extended Crafting's custom table recipe engine without a hard dependency. */
@Pseudo
@Mixin(targets = "com.blakebr0.extendedcrafting.crafting.table.TableRecipeManager", remap = false)
public abstract class ExtendedTableRecipeManagerMixin implements ExtendedTableRecipeManagerBridge {

    @Shadow(remap = false)
    public abstract List<IRecipe> getRecipes();

    @Shadow(remap = false)
    public abstract ItemStack findMatchingRecipe(InventoryCrafting grid, World world);

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$captureManager(CallbackInfo callbackInfo) {
        ExtendedCraftingBridgeRegistry.setRecipeManager(this);
    }

    @Inject(
            method = "findMatchingRecipe",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private void retropolymorph$resolveSelectedOutput(
            InventoryCrafting grid,
            World world,
            CallbackInfoReturnable<ItemStack> callbackInfo) {
        if (!PolymorphConfig.isIntegrationExtendedCraftingEnabled()) {
            return;
        }
        IRecipe selected = ExtendedTableSelectionResolver.resolve(grid, world, this);
        if (selected != null) {
            callbackInfo.setReturnValue(selected.getCraftingResult(grid));
        }
    }

    @Inject(
            method = "getRemainingItems",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private static void retropolymorph$resolveSelectedRemainingItems(
            InventoryCrafting grid,
            World world,
            CallbackInfoReturnable<NonNullList<ItemStack>> callbackInfo) {
        if (!PolymorphConfig.isIntegrationExtendedCraftingEnabled()) {
            return;
        }
        ExtendedTableRecipeManagerBridge manager = ExtendedCraftingBridgeRegistry.getRecipeManager();
        if (manager == null) {
            return;
        }

        IRecipe selected = ExtendedTableSelectionResolver.resolve(grid, world, manager);
        if (selected != null) {
            callbackInfo.setReturnValue(selected.getRemainingItems(grid));
        }
    }

    @Override
    public List<IRecipe> retropolymorph$getTableRecipes() {
        return this.getRecipes();
    }

    @Override
    public ItemStack retropolymorph$findTableResult(InventoryCrafting matrix, World world) {
        return this.findMatchingRecipe(matrix, world);
    }
}
