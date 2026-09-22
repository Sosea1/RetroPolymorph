package dev.sosea1.retropolymorph.mixin.compat.extendedcrafting;

import com.blakebr0.extendedcrafting.crafting.endercrafter.IEnderCraftingRecipe;
import com.blakebr0.extendedcrafting.crafting.table.TableCrafting;
import dev.sosea1.retropolymorph.compat.extendedcrafting.EnderCrafterRecipeIdentity;
import dev.sosea1.retropolymorph.compat.extendedcrafting.EnderCrafterSelectionAccess;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

@Pseudo
@Mixin(targets = "com.blakebr0.extendedcrafting.crafting.endercrafter.EnderCrafterRecipeManager", remap = false)
public abstract class EnderCrafterRecipeManagerMixin {

    @Unique
    private static final Field RETROPOLYMORPH$TILE_FIELD;

    static {
        Field field = null;
        try {
            Class<?> clazz = Class.forName("com.blakebr0.extendedcrafting.crafting.table.TableCrafting");
            field = clazz.getField("tile");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        RETROPOLYMORPH$TILE_FIELD = field;
    }

    @Shadow(remap = false)
    private List recipes;

    @Inject(method = "findMatchingRecipe", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void retropolymorph$resolveSelectedEnderRecipe(
            TableCrafting matrix,
            World world,
            CallbackInfoReturnable<IEnderCraftingRecipe> cir) {
        if (!PolymorphConfig.isIntegrationExtendedCraftingEnabled() || matrix == null || world == null) {
            return;
        }

        Object tile = retropolymorph$extractTile(matrix);
        if (tile instanceof EnderCrafterSelectionAccess) {
            EnderCrafterSelectionAccess access = (EnderCrafterSelectionAccess) tile;
            int selectedIndex = access.retropolymorph$getEnderCrafterRecipeIndex();
            net.minecraft.item.ItemStack expectedOutput = access.retropolymorph$getExpectedOutput();
            if (selectedIndex >= 0 && this.recipes != null) {
                // 1. Try candidate at saved index
                if (selectedIndex < this.recipes.size()) {
                    Object obj = this.recipes.get(selectedIndex);
                    if (obj instanceof IRecipe && obj instanceof IEnderCraftingRecipe) {
                        IRecipe recipe = (IRecipe) obj;
                        boolean outputMatches = (expectedOutput == null || expectedOutput.isEmpty())
                                || EnderCrafterRecipeIdentity.matchesExpectedOutput(recipe, matrix, expectedOutput);
                        if (outputMatches && RecipeProbe.matches(recipe, matrix, world)) {
                            cir.setReturnValue((IEnderCraftingRecipe) recipe);
                            return;
                        }
                    }
                }

                // 2. Fallback: if recipes shifted, try finding recipe matching expectedOutput and matrix
                if (expectedOutput != null && !expectedOutput.isEmpty()) {
                    for (int i = 0; i < this.recipes.size(); i++) {
                        Object obj = this.recipes.get(i);
                        if (obj instanceof IRecipe && obj instanceof IEnderCraftingRecipe) {
                            IRecipe recipe = (IRecipe) obj;
                            if (EnderCrafterRecipeIdentity.matchesExpectedOutput(recipe, matrix, expectedOutput)
                                    && RecipeProbe.matches(recipe, matrix, world)) {
                                access.retropolymorph$setEnderCrafterRecipe(i, expectedOutput);
                                cir.setReturnValue((IEnderCraftingRecipe) recipe);
                                return;
                            }
                        }
                    }
                }

                // 3. Stale selection: clear safely to prevent crafting wrong recipe
                access.retropolymorph$setEnderCrafterRecipe(-1, null);
            }
        }
    }

    @Unique
    @Nullable
    private static Object retropolymorph$extractTile(TableCrafting matrix) {
        if (RETROPOLYMORPH$TILE_FIELD != null) {
            try {
                return RETROPOLYMORPH$TILE_FIELD.get(matrix);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            }
        }
        return null;
    }
}
