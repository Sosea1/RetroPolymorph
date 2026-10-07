package com.sosea1.retropolymorph.mixin.compat.rftools;

import com.sosea1.retropolymorph.compat.rftools.RFToolsPreviewSelection;
import com.sosea1.retropolymorph.compat.rftools.RFToolsRecipeSelectionAccess;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "mcjty.rftools.blocks.crafter.GuiCrafter", remap = false)
public abstract class RFToolsGuiCrafterMixin extends GuiContainer {
    protected RFToolsGuiCrafterMixin(Container container) { super(container); }

    @Redirect(method = {"testRecipe", "applyRecipe"}, at = @At(value = "INVOKE",
            target = "Lmcjty/rftools/craftinggrid/CraftingRecipe;findRecipe(Lnet/minecraft/world/World;Lnet/minecraft/inventory/InventoryCrafting;)Lnet/minecraft/item/crafting/IRecipe;"), require = 2)
    private IRecipe retropolymorph$resolvePreview(World world, InventoryCrafting matrix) {
        IRecipe selected = RFToolsPreviewSelection.resolve(
                RFToolsPreviewSelection.get(this.inventorySlots.getSlot(0).inventory), matrix, world);
        if (selected != null) { return selected; }
        // RFTools uses registry order directly, without Forge's repair special case.
        for (IRecipe recipe : net.minecraft.item.crafting.CraftingManager.REGISTRY) {
            if (com.sosea1.retropolymorph.core.RecipeProbe.matches(recipe, matrix, world)) { return recipe; }
        }
        return null;
    }

    @Redirect(method = "selectRecipe", at = @At(value = "INVOKE",
            target = "Lmcjty/rftools/craftinggrid/CraftingRecipe;getResult()Lnet/minecraft/item/ItemStack;"), require = 1)
    private ItemStack retropolymorph$restoreEditorChoice(@Coerce Object saved) {
        RFToolsRecipeSelectionAccess access = (RFToolsRecipeSelectionAccess) saved;
        RFToolsPreviewSelection.set(this.inventorySlots.getSlot(0).inventory, access.retropolymorph$getSelectedRecipeId());
        Object tile = this.inventorySlots.getSlot(0).inventory;
        try {
            int size = (Integer) tile.getClass().getMethod("getSupportedRecipes").invoke(tile);
            java.lang.reflect.Method getRecipe = tile.getClass().getMethod("getRecipe", int.class);
            for (int i = 0; i < size; i++) {
                if (getRecipe.invoke(tile, i) == saved) {
                    com.sosea1.retropolymorph.network.NetworkHandler.openRFToolsTemplate(this.inventorySlots.windowId, i);
                    break;
                }
            }
        } catch (ReflectiveOperationException exception) {
            com.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure("rftools", "openSavedTemplate", exception);
        }
        return access.retropolymorph$getOutput();
    }
}
