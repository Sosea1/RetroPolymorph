package dev.sosea1.retropolymorph.compat.thaumcraft;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

import javax.annotation.Nullable;

/** Runtime resolver called only from Thaumcraft-gated mixins. */
public final class ThaumcraftArcaneSelectionResolver {

    private ThaumcraftArcaneSelectionResolver() {
    }

    /** Returns the explicit arcane selection, or null without falling back. */
    @Nullable
    public static IArcaneRecipe resolveSelected(
            InventoryCrafting matrix,
            EntityPlayer player) {
        if (!PolymorphConfig.isIntegrationThaumcraftEnabled()) {
            return null;
        }

        ResourceLocation selected = ThaumcraftArcaneSelectionStore.get(matrix, player);
        if (selected == null) {
            return null;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selected);
        if (recipe == null) {
            ThaumcraftArcaneSelectionStore.clear(matrix, player);
            return null;
        }
        if (!(recipe instanceof IArcaneRecipe)) {
            // A valid vanilla fallback selection intentionally makes the arcane
            // lookup return null. Do not erase it here.
            return null;
        }
        if (!ThaumcraftArcaneReflection.isRecipeSelectable(recipe, player)) {
            ThaumcraftArcaneSelectionStore.clear(matrix, player);
            return null;
        }

        try {
            if (!RecipeProbe.matches(recipe, matrix, player.world)) {
                ThaumcraftArcaneSelectionStore.clear(matrix, player);
                return null;
            }
        } catch (RuntimeException | LinkageError exception) {
            ThaumcraftArcaneSelectionStore.clear(matrix, player);
            return null;
        }

        ThaumcraftArcaneSelectionStore.markObserved(matrix, player, selected);
        return (IArcaneRecipe) recipe;
    }

    /**
     * Returns explicit arcane selection, suppresses native arcane matching for a
     * valid explicit vanilla selection, otherwise falls back to Thaumcraft.
     */
    @Nullable
    public static IArcaneRecipe resolve(
            InventoryCrafting matrix,
            EntityPlayer player) {
        if (!PolymorphConfig.isIntegrationThaumcraftEnabled()) {
            return ThaumcraftCraftingManager.findMatchingArcaneRecipe(matrix, player);
        }

        ResourceLocation selected = ThaumcraftArcaneSelectionStore.get(matrix, player);
        if (selected == null) {
            return ThaumcraftCraftingManager.findMatchingArcaneRecipe(matrix, player);
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selected);
        if (recipe == null) {
            ThaumcraftArcaneSelectionStore.clear(matrix, player);
            return ThaumcraftCraftingManager.findMatchingArcaneRecipe(matrix, player);
        }

        if (recipe instanceof IArcaneRecipe) {
            IArcaneRecipe arcane = resolveSelected(matrix, player);
            return arcane != null
                    ? arcane
                    : ThaumcraftCraftingManager.findMatchingArcaneRecipe(matrix, player);
        }

        InventoryCrafting vanilla = ThaumcraftCraftingMatrices.copyVanillaMatrix(matrix);
        if (isValidVanillaSelection(recipe, vanilla, player, player.world)) {
            // This is the key cross-engine switch: let Thaumcraft enter its own
            // vanilla fallback branch instead of reproducing that branch here.
            return null;
        }

        ThaumcraftArcaneSelectionStore.clear(matrix, player);
        return ThaumcraftCraftingManager.findMatchingArcaneRecipe(matrix, player);
    }

    /** Resolves the vanilla recipe inside Thaumcraft's native fallback branch. */
    @Nullable
    public static IRecipe resolveVanillaFallback(
            InventoryCrafting arcaneMatrix,
            InventoryCrafting vanillaMatrix,
            EntityPlayer player,
            World world) {
        if (PolymorphConfig.isIntegrationThaumcraftEnabled()) {
            IRecipe selected = resolveSelectedVanilla(arcaneMatrix, vanillaMatrix, player, world);
            if (selected != null) {
                ResourceLocation id = selected.getRegistryName();
                if (id != null) {
                    ThaumcraftArcaneSelectionStore.markObserved(arcaneMatrix, player, id);
                }
                return selected;
            }
        }
        return CraftingManager.findMatchingRecipe(vanillaMatrix, world);
    }

    /** Returns explicit vanilla selection when valid, never a native fallback. */
    @Nullable
    public static IRecipe resolveSelectedVanilla(
            InventoryCrafting arcaneMatrix,
            InventoryCrafting vanillaMatrix,
            EntityPlayer player,
            World world) {
        ResourceLocation selected = ThaumcraftArcaneSelectionStore.get(arcaneMatrix, player);
        if (selected == null) {
            return null;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(selected);
        if (recipe == null) {
            ThaumcraftArcaneSelectionStore.clear(arcaneMatrix, player);
            return null;
        }
        if (recipe instanceof IArcaneRecipe) {
            return null;
        }
        if (!isValidVanillaSelection(recipe, vanillaMatrix, player, world)) {
            ThaumcraftArcaneSelectionStore.clear(arcaneMatrix, player);
            return null;
        }
        return recipe;
    }

    private static boolean isValidVanillaSelection(
            IRecipe recipe,
            InventoryCrafting vanillaMatrix,
            EntityPlayer player,
            World world) {
        if (!ThaumcraftArcaneReflection.isVanillaRecipeSelectable(recipe, player)) {
            return false;
        }
        try {
            return RecipeProbe.matches(recipe, vanillaMatrix, world);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }
}
