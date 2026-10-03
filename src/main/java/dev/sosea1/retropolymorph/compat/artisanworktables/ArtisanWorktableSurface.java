package dev.sosea1.retropolymorph.compat.artisanworktables;

import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionScope;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.CraftingRecipeOptionCollector;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

final class ArtisanWorktableSurface implements MachineRecipeSurface {

    private final ArtisanWorktableReflection.Binding binding;
    private boolean customMatchKnown;
    private boolean hasCustomMatch;

    ArtisanWorktableSurface(ArtisanWorktableReflection.Binding binding) {
        this.binding = binding;
    }

    @Override
    public Container getContainer() {
        return this.binding.container;
    }

    @Override
    public Object getRecipeOwner() {
        return this.binding.tile;
    }

    @Override
    public SelectionScope getSelectionScope() {
        return SelectionScope.local();
    }

    @Override
    public int getInputCount() {
        return this.binding.matrixHandler.getSlots();
    }

    @Override
    public ItemStack getInputStack(int index) {
        return index >= 0 && index < this.binding.matrixHandler.getSlots()
                ? this.binding.matrixHandler.getStackInSlot(index)
                : ItemStack.EMPTY;
    }

    @Override
    public int getClientStateToken() {
        return ArtisanWorktableReflection.clientStateToken(this.binding);
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        if (world == null) {
            return Collections.emptyList();
        }

        List<RecipeOption> custom = ArtisanWorktableReflection.findCustomOptions(this.binding);
        this.customMatchKnown = true;
        this.hasCustomMatch = !custom.isEmpty();
        if (this.hasCustomMatch) {
            return custom;
        }
        if (!ArtisanWorktableReflection.allowsVanillaCrafting(this.binding.tile)) {
            return Collections.emptyList();
        }

        InventoryCrafting matrix = this.binding.vanillaMatrix;
        return CraftingRecipeOptionCollector.collectForge(
                matrix,
                RecipeResolver.findAllMatches(matrix, world));
    }

    @Override
    public boolean selectRecipe(String recipeKey, World world) {
        if (world == null || !RecipeKey.isWireSafe(recipeKey)) {
            return false;
        }

        if (ArtisanWorktableReflection.selectCustom(this.binding, recipeKey)) {
            RecipeSelectionSeeder.clear(this.binding.vanillaMatrix);
            ArtisanWorktableReflection.invalidateVanillaRecipeCache();
            return true;
        }

        if (hasCustomMatch()
                || !ArtisanWorktableReflection.allowsVanillaCrafting(this.binding.tile)) {
            return false;
        }

        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        IRecipe recipe = id == null ? null : ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null || !RecipeProbe.matches(recipe, this.binding.vanillaMatrix, world)) {
            return false;
        }

        ArtisanWorktableReflection.clearCustom(this.binding);
        RecipeSelectionSeeder.seed(this.binding.vanillaMatrix, id);
        ArtisanWorktableReflection.invalidateVanillaRecipeCache();
        return true;
    }

    @Override
    public void clearRecipeSelection() {
        ArtisanWorktableReflection.clearCustom(this.binding);
        RecipeSelectionSeeder.clear(this.binding.vanillaMatrix);
        ArtisanWorktableReflection.invalidateVanillaRecipeCache();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        String custom = ArtisanWorktableReflection.getSelectedCustomKey(this.binding);
        if (custom != null) {
            return custom;
        }

        if (hasCustomMatch()) {
            RecipeSelectionSeeder.clear(this.binding.vanillaMatrix);
            return null;
        }

        if (!(this.binding.vanillaMatrix instanceof CraftingMatrixExtension)) {
            return null;
        }
        RecipeSelectionState state = ((CraftingMatrixExtension) this.binding.vanillaMatrix)
                .retropolymorph$peekRecipeSelectionState();
        ResourceLocation id = state == null ? null : state.getSelectedRecipeId();
        return id == null ? null : id.toString();
    }

    private boolean hasCustomMatch() {
        if (!this.customMatchKnown) {
            this.hasCustomMatch = ArtisanWorktableReflection.hasAnyCustomMatch(this.binding);
            this.customMatchKnown = true;
        }
        return this.hasCustomMatch;
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        ArtisanWorktableReflection.clearCustom(this.binding);
        RecipeSelectionSeeder.clear(this.binding.vanillaMatrix);

        if (recipeKey != null && ArtisanWorktableReflection.isCustomKey(recipeKey)) {
            ArtisanWorktableReflection.setRemoteCustom(this.binding, recipeKey);
        } else {
            RecipeSelectionSeeder.seed(
                    this.binding.vanillaMatrix,
                    RecipeKey.parseForgeId(recipeKey));
        }
        ArtisanWorktableReflection.invalidateVanillaRecipeCache();
    }

    @Override
    public void refreshPreview() {
        ArtisanWorktableReflection.refresh(this.binding.container);
    }

    @Override
    public MachineRecipePersistence getPersistencePolicy() {
        return MachineRecipePersistence.PLAYER_PROFILE;
    }

    @Override
    public boolean controlsActualOperation() {
        return true;
    }

    @Override
    public Slot getResultSlot() {
        return this.binding.resultSlot;
    }
}
