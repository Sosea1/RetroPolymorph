package dev.sosea1.retropolymorph.compat.refinedstorage;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lazily places a pattern-pinned recipe before RS's normal registry order.
 *
 * CraftingPattern still performs its own recipe.matches(savedMatrix, world)
 * check. An invalid/stale/forged id therefore falls through to stock RS order.
 */
public final class RefinedStoragePreferredRecipeIterator implements Iterator<IRecipe> {

    private final Iterator<IRecipe> delegate;
    private final IRecipe preferred;
    private boolean preferredPending;
    private IRecipe buffered;
    private boolean bufferedReady;

    private RefinedStoragePreferredRecipeIterator(
            Iterator<IRecipe> delegate,
            IRecipe preferred) {
        this.delegate = delegate;
        this.preferred = preferred;
        this.preferredPending = preferred != null;
    }

    public static Iterator<IRecipe> wrap(Iterator<IRecipe> delegate, ItemStack pattern) {
        if (delegate == null) {
            throw new NullPointerException("delegate");
        }

        ResourceLocation recipeId = RefinedStoragePatternData.readSelectedRecipe(pattern);
        IRecipe preferred = recipeId == null ? null : ForgeRegistries.RECIPES.getValue(recipeId);
        return preferred == null
                ? delegate
                : new RefinedStoragePreferredRecipeIterator(delegate, preferred);
    }

    @Override
    public boolean hasNext() {
        if (this.preferredPending) {
            return true;
        }
        prepareBuffered();
        return this.bufferedReady;
    }

    @Override
    public IRecipe next() {
        if (this.preferredPending) {
            this.preferredPending = false;
            return this.preferred;
        }

        prepareBuffered();
        if (!this.bufferedReady) {
            throw new NoSuchElementException();
        }

        IRecipe next = this.buffered;
        this.buffered = null;
        this.bufferedReady = false;
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void prepareBuffered() {
        if (this.bufferedReady) {
            return;
        }

        while (this.delegate.hasNext()) {
            IRecipe candidate = this.delegate.next();
            if (candidate == this.preferred) {
                continue;
            }
            this.buffered = candidate;
            this.bufferedReady = true;
            return;
        }
    }
}
