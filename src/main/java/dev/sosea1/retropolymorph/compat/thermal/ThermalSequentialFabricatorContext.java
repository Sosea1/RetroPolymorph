package dev.sosea1.retropolymorph.compat.thermal;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.core.CraftingRecipeOptionCollector;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Focused recipe-selection context for Thermal Expansion's Sequential Fabricator. */
final class ThermalSequentialFabricatorContext implements RecipeSelectionContext {

    private final Container container;
    private final InventoryCrafting matrix;
    private final Slot resultSlot;
    private final TileEntity tile;

    ThermalSequentialFabricatorContext(
            Container container,
            InventoryCrafting matrix,
            Slot resultSlot,
            TileEntity tile) {
        this.container = container;
        this.matrix = matrix;
        this.resultSlot = resultSlot;
        this.tile = tile;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public InventoryCrafting getRecipeMatrix() {
        return this.matrix;
    }

    @Override
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        List<IRecipe> matches = RecipeResolver.findAllMatches(this.matrix, world);
        if (matches.isEmpty()) {
            return matches;
        }
        ArrayList<IRecipe> supported = new ArrayList<IRecipe>(matches.size());
        for (IRecipe recipe : matches) {
            if (ThermalSequentialFabricatorRecipeSupport.isSupported(recipe)) {
                supported.add(recipe);
            }
        }
        return supported.isEmpty()
                ? Collections.<IRecipe>emptyList()
                : Collections.unmodifiableList(supported);
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        if (RecipeSelectionContext.isEmpty(this.matrix)) {
            return Collections.emptyList();
        }
        return CraftingRecipeOptionCollector.collect(
                this,
                this.matrix,
                findAllMatches(world));
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation id = recipe == null ? null : recipe.getRegistryName();
        return id == null ? null : id.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id == null) {
            return false;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null
                || !ThermalSequentialFabricatorRecipeSupport.isSupported(recipe)
                || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }

        // Selecting in ContainerCrafter changes only its ghost preview. The
        // real machine recipe is committed later by Thermal's Set Recipe
        // button/mode packet; committing here would desync a running machine.
        RecipeSelectionSeeder.seed(this.matrix, id);
        // Use Thermal's normal ContainerCrafter callback. Its own preview code
        // still owns CraftResult synchronization and recipe validity handling.
        this.container.onCraftMatrixChanged(this.matrix);
        return true;
    }

    @Override
    public void clearSelection() {
        if (this.matrix instanceof CraftingMatrixExtension) {
            RecipeSelectionState state = ((CraftingMatrixExtension) this.matrix)
                    .retropolymorph$peekRecipeSelectionState();
            if (state != null) {
                state.clear();
            }
        }
        this.container.onCraftMatrixChanged(this.matrix);
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        if (!(this.matrix instanceof CraftingMatrixExtension)) {
            return null;
        }
        RecipeSelectionState state = ((CraftingMatrixExtension) this.matrix)
                .retropolymorph$peekRecipeSelectionState();
        ResourceLocation id = state == null ? null : state.getSelectedRecipeId();
        if (id == null) {
            return null;
        }
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        World world = this.tile.getWorld();
        return recipe != null
                && world != null
                && ThermalSequentialFabricatorRecipeSupport.isSupported(recipe)
                && RecipeProbe.matches(recipe, this.matrix, world)
                ? id.toString()
                : null;
    }

    @Override
    public SelectionPersistencePolicy getPersistencePolicy() {
        // The recipe definition belongs to the shared machine, not to one
        // player. Durable per-player restoration would make users fight over it.
        return SelectionPersistencePolicy.OWNER_ONLY;
    }
}
