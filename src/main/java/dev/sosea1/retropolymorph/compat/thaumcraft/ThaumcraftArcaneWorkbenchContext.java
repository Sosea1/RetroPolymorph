package dev.sosea1.retropolymorph.compat.thaumcraft;

import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unified Thaumcraft 6 Arcane Workbench selection context.
 *
 * Thaumcraft gives arcane recipes precedence and falls back to a temporary 3x3
 * vanilla matrix only when no arcane recipe is chosen. This context exposes
 * both engines while preserving that native execution path.
 */
final class ThaumcraftArcaneWorkbenchContext implements RecipeSelectionContext {

    private final Container container;
    private final InventoryCrafting matrix;
    private final EntityPlayer player;
    private final Slot resultSlot;

    ThaumcraftArcaneWorkbenchContext(
            Container container,
            InventoryCrafting matrix,
            EntityPlayer player,
            Slot resultSlot) {
        this.container = container;
        this.matrix = matrix;
        this.player = player;
        this.resultSlot = resultSlot;
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
    public dev.sosea1.retropolymorph.api.SelectorPlacement getSelectorPlacement() {
        return dev.sosea1.retropolymorph.api.SelectorPlacement.resultSlot(
                this.resultSlot.xPos,
                this.resultSlot.yPos,
                0,
                -14);
    }

    @Override
    public List<IRecipe> findAllMatches(World world) {
        if (world == null) {
            return Collections.emptyList();
        }
        InventoryCrafting vanillaMatrix = ThaumcraftCraftingMatrices.copyVanillaMatrix(this.matrix);
        ArrayList<IRecipe> matches = new ArrayList<IRecipe>();
        appendArcaneMatches(matches, world);
        appendVanillaMatches(matches, vanillaMatrix, world);
        return matches.isEmpty()
                ? Collections.<IRecipe>emptyList()
                : Collections.unmodifiableList(matches);
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        if (world == null || isCraftingAreaEmpty()) {
            return Collections.emptyList();
        }

        InventoryCrafting vanillaMatrix = ThaumcraftCraftingMatrices.copyVanillaMatrix(this.matrix);
        ArrayList<OptionEntry> accepted = new ArrayList<OptionEntry>(8);

        // Arcane recipes intentionally come first: this mirrors Thaumcraft's native precedence.
        for (IRecipe recipe : ThaumcraftArcaneReflection.getArcaneRecipes()) {
            if (recipe == null || !ThaumcraftArcaneReflection.isRecipeSelectable(recipe, this.player)) {
                continue;
            }
            try {
                if (RecipeProbe.matches(recipe, this.matrix, world)) {
                    addOption(accepted, recipe, this.matrix, true);
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Broken third-party recipes must not take down the selector.
            }
        }

        for (IRecipe recipe : RecipeResolver.findAllMatches(vanillaMatrix, world)) {
            if (recipe == null
                    || ThaumcraftArcaneReflection.isArcaneRecipe(recipe)
                    || !ThaumcraftArcaneReflection.isVanillaRecipeSelectable(recipe, this.player)) {
                continue;
            }
            addOption(accepted, recipe, vanillaMatrix, false);
        }

        if (accepted.size() <= 1) {
            return Collections.emptyList();
        }
        ArrayList<RecipeOption> result = new ArrayList<RecipeOption>(accepted.size());
        for (OptionEntry entry : accepted) {
            result.add(entry.option);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    @Nullable
    public String getRecipeKey(IRecipe recipe) {
        ResourceLocation id = recipe.getRegistryName();
        return id == null ? null : id.toString();
    }

    @Override
    public boolean select(String recipeKey, World world) {
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id == null || world == null) {
            return false;
        }

        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        if (!isSelectableMatch(recipe, world)) {
            return false;
        }

        ThaumcraftArcaneSelectionStore.select(this.matrix, this.player, id);
        this.container.onCraftMatrixChanged(this.matrix);

        boolean observed = ThaumcraftArcaneSelectionStore.wasObserved(
                this.matrix, this.player, id);
        if (!observed) {
            ThaumcraftArcaneSelectionStore.clear(this.matrix, this.player);
            this.container.onCraftMatrixChanged(this.matrix);
        }
        return observed;
    }

    @Override
    public void clearSelection() {
        if (ThaumcraftArcaneSelectionStore.get(this.matrix, this.player) == null) {
            return;
        }
        ThaumcraftArcaneSelectionStore.clear(this.matrix, this.player);
        this.container.onCraftMatrixChanged(this.matrix);
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation id = ThaumcraftArcaneSelectionStore.get(this.matrix, this.player);
        return id == null ? null : id.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id == null) {
            ThaumcraftArcaneSelectionStore.clear(this.matrix, this.player);
        } else {
            ThaumcraftArcaneSelectionStore.select(this.matrix, this.player, id);
        }
    }

    private void appendArcaneMatches(List<IRecipe> matches, World world) {
        for (IRecipe recipe : ThaumcraftArcaneReflection.getArcaneRecipes()) {
            if (recipe == null || !ThaumcraftArcaneReflection.isRecipeSelectable(recipe, this.player)) {
                continue;
            }
            try {
                if (RecipeProbe.matches(recipe, this.matrix, world)) {
                    matches.add(recipe);
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }

    private void appendVanillaMatches(
            List<IRecipe> matches,
            InventoryCrafting vanillaMatrix,
            World world) {
        for (IRecipe recipe : RecipeResolver.findAllMatches(vanillaMatrix, world)) {
            if (recipe != null
                    && !ThaumcraftArcaneReflection.isArcaneRecipe(recipe)
                    && ThaumcraftArcaneReflection.isVanillaRecipeSelectable(recipe, this.player)) {
                matches.add(recipe);
            }
        }
    }

    private boolean isSelectableMatch(@Nullable IRecipe recipe, World world) {
        if (recipe == null) {
            return false;
        }
        try {
            if (ThaumcraftArcaneReflection.isArcaneRecipe(recipe)) {
                return ThaumcraftArcaneReflection.isRecipeSelectable(recipe, this.player)
                        && RecipeProbe.matches(recipe, this.matrix, world);
            }
            InventoryCrafting vanillaMatrix = ThaumcraftCraftingMatrices.copyVanillaMatrix(this.matrix);
            return ThaumcraftArcaneReflection.isVanillaRecipeSelectable(recipe, this.player)
                    && RecipeProbe.matches(recipe, vanillaMatrix, world);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private void addOption(
            List<OptionEntry> accepted,
            IRecipe recipe,
            InventoryCrafting remainderMatrix,
            boolean arcane) {
        String key = getRecipeKey(recipe);
        if (!RecipeKey.isWireSafe(key)) {
            return;
        }

        // Thaumcraft itself calls getCraftingResult on the 5x3 workbench even
        // for its vanilla fallback, so the selector mirrors that exact behavior.
        ItemStack output = RecipeProbe.craftingResult(recipe, this.matrix);
        if (output == null || output.isEmpty()) {
            return;
        }

        // Never collapse arcane choices: equal output/remainders can still have
        // different research, vis or crystal costs. Never collapse across engines.
        if (!arcane && containsEquivalentVanilla(accepted, recipe, output, remainderMatrix)) {
            return;
        }
        accepted.add(new OptionEntry(recipe, remainderMatrix, arcane, new RecipeOption(key, output)));
    }

    private static boolean containsEquivalentVanilla(
            List<OptionEntry> accepted,
            IRecipe recipe,
            ItemStack output,
            InventoryCrafting matrix) {
        for (OptionEntry existing : accepted) {
            if (existing.arcane || !ItemStack.areItemStacksEqual(existing.option.getOutput(), output)) {
                continue;
            }
            if (sameRemainders(existing.recipe, existing.remainderMatrix, recipe, matrix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameRemainders(
            IRecipe first,
            InventoryCrafting firstMatrix,
            IRecipe second,
            InventoryCrafting secondMatrix) {
        NonNullList<ItemStack> a;
        NonNullList<ItemStack> b;
        try {
            a = first.getRemainingItems(firstMatrix);
        } catch (RuntimeException | LinkageError exception) {
            RecipeProbe.reportRemainderFailure(first, exception);
            return false;
        }
        try {
            b = second.getRemainingItems(secondMatrix);
        } catch (RuntimeException | LinkageError exception) {
            RecipeProbe.reportRemainderFailure(second, exception);
            return false;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int slot = 0; slot < a.size(); slot++) {
            ItemStack left = a.get(slot) == null ? ItemStack.EMPTY : a.get(slot);
            ItemStack right = b.get(slot) == null ? ItemStack.EMPTY : b.get(slot);
            if (!ItemStack.areItemStacksEqual(left, right)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCraftingAreaEmpty() {
        int limit = Math.min(9, this.matrix.getSizeInventory());
        for (int slot = 0; slot < limit; slot++) {
            if (!this.matrix.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static final class OptionEntry {
        private final IRecipe recipe;
        private final InventoryCrafting remainderMatrix;
        private final boolean arcane;
        private final RecipeOption option;

        private OptionEntry(
                IRecipe recipe,
                InventoryCrafting remainderMatrix,
                boolean arcane,
                RecipeOption option) {
            this.recipe = recipe;
            this.remainderMatrix = remainderMatrix;
            this.arcane = arcane;
            this.option = option;
        }
    }
}
