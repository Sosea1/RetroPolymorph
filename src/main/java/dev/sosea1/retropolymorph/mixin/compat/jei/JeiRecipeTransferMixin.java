package dev.sosea1.retropolymorph.mixin.compat.jei;

import dev.sosea1.retropolymorph.client.JeiRecipeIdentity;
import dev.sosea1.retropolymorph.client.JeiTransferIntent;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import mezz.jei.gui.ingredients.GuiIngredient;
import mezz.jei.gui.ingredients.GuiItemStackGroup;
import mezz.jei.gui.recipes.RecipeLayout;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Captures successful JEI 1.12 transfer intent without changing JEI's packet flow. */
@Pseudo
@Mixin(targets = "mezz.jei.transfer.RecipeTransferUtil", remap = false)
public abstract class JeiRecipeTransferMixin {

    private static final int MAX_RAW_OUTPUTS = 64;

    @Inject(
            method = "transferRecipe(Lnet/minecraft/inventory/Container;"
                    + "Lmezz/jei/gui/recipes/RecipeLayout;"
                    + "Lnet/minecraft/entity/player/EntityPlayer;ZZ)"
                    + "Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At("RETURN"),
            remap = false,
            require = 0)
    private static void retropolymorph$captureSuccessfulTransfer(
            Container container,
            RecipeLayout layout,
            EntityPlayer player,
            boolean maxTransfer,
            boolean doTransfer,
            CallbackInfoReturnable<Object> cir) {
        if (!PolymorphConfig.isIntegrationJeiEnabled() || !doTransfer || cir.getReturnValue() != null) {
            return;
        }

        GuiItemStackGroup itemStacks = layout.getItemStacks();
        Map<Integer, GuiIngredient> ingredients = itemStacks.getGuiIngredients();
        ArrayList<ItemStack> outputs = new ArrayList<ItemStack>();
        outputLoop:
        for (GuiIngredient ingredient : ingredients.values()) {
            if (ingredient == null || ingredient.isInput()) {
                continue;
            }
            Object displayed = ingredient.getDisplayedIngredient();
            if (displayed instanceof ItemStack) {
                outputs.add((ItemStack) displayed);
                if (outputs.size() >= MAX_RAW_OUTPUTS) {
                    break;
                }
            }
            List<?> values = ingredient.getAllIngredients();
            if (values == null) {
                continue;
            }
            for (Object value : values) {
                if (value instanceof ItemStack) {
                    outputs.add((ItemStack) value);
                    if (outputs.size() >= MAX_RAW_OUTPUTS) {
                        break outputLoop;
                    }
                }
            }
        }
        JeiTransferIntent.capture(container, JeiRecipeIdentity.tryExtractRecipeKey(layout), outputs);
    }
}
