package dev.sosea1.retropolymorph.mixin.compat.refinedstorage;

import dev.sosea1.retropolymorph.compat.refinedstorage.RefinedStoragePreferredRecipeIterator;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

/**
 * Makes RS 1.12 autocrafting honor a recipe id encoded by Pattern Grid.
 *
 * The selected recipe is only moved to the front; RS itself still calls
 * IRecipe.matches on the saved 3x3 inputs before accepting it. If validation
 * fails, iteration transparently continues in stock registry order.
 */
@Pseudo
@Mixin(targets = "com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern", remap = false)
public abstract class RefinedStorageCraftingPatternMixin {

    @Shadow(remap = false)
    private ItemStack stack;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/registry/RegistryNamespaced;iterator()Ljava/util/Iterator;",
                    ordinal = 0,
                    remap = false),
            remap = false,
            require = 1)
    private Iterator<IRecipe> retropolymorph$preferEncodedRecipe(
            RegistryNamespaced<ResourceLocation, IRecipe> registry) {
        return PolymorphConfig.isIntegrationRefinedStorageEnabled()
                ? RefinedStoragePreferredRecipeIterator.wrap(registry.iterator(), this.stack)
                : registry.iterator();
    }
}
