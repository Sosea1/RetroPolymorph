package dev.sosea1.retropolymorph.machine;

import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectionScope;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Common SelectionContext wrapper for one opt-in machine recipe surface. */
public final class MachineSelectionContext implements SelectionContext {

    private final MachineRecipeSurface surface;

    MachineSelectionContext(MachineRecipeSurface surface) {
        this.surface = surface;
    }

    public Object getRecipeOwner() {
        return this.surface.getRecipeOwner();
    }

    public MachineRecipePersistence getMachinePersistencePolicy() {
        return this.surface.getPersistencePolicy();
    }

    @Override
    public SelectionPersistencePolicy getPersistencePolicy() {
        MachineRecipePersistence policy = this.surface.getPersistencePolicy();
        if (policy == MachineRecipePersistence.PLAYER_PROFILE) {
            return SelectionPersistencePolicy.PLAYER_PERSISTENT;
        } else if (policy == MachineRecipePersistence.OWNER_WITH_PLAYER_PROFILE) {
            return SelectionPersistencePolicy.PLAYER_PERSISTENT_OVERRIDE;
        } else {
            return SelectionPersistencePolicy.OWNER_ONLY;
        }
    }

    @Override
    public Container getContainer() {
        return this.surface.getContainer();
    }

    @Override
    @Nullable
    public Slot getResultSlot() {
        return this.surface.getResultSlot();
    }

    @Override
    public int getInputCount() {
        return Math.max(0, this.surface.getInputCount());
    }

    @Override
    public ItemStack getInputStack(int index) {
        ItemStack stack = this.surface.getInputStack(index);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public int getClientStateToken() {
        return this.surface.getClientStateToken();
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        List<RecipeOption> raw = this.surface.findOptions(world);
        if (raw == null || raw.size() <= 1) {
            return Collections.emptyList();
        }

        ArrayList<RecipeOption> safe = new ArrayList<RecipeOption>(raw.size());
        Set<String> seen = new HashSet<String>();
        for (RecipeOption option : raw) {
            if (option == null
                    || !RecipeKey.isWireSafe(option.getRecipeKey())
                    || option.getOutput().isEmpty()
                    || !seen.add(option.getRecipeKey())) {
                continue;
            }
            safe.add(new RecipeOption(option.getRecipeKey(), option.getOutput()));
        }
        return safe.size() <= 1
                ? Collections.<RecipeOption>emptyList()
                : Collections.unmodifiableList(safe);
    }

    @Override
    public boolean select(String recipeKey, World world) {
        boolean selected = this.surface.selectRecipe(recipeKey, world);
        if (selected) {
            this.surface.refreshPreview();
        }
        return selected;
    }

    @Override
    public void clearSelection() {
        this.surface.clearRecipeSelection();
        this.surface.refreshPreview();
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        return this.surface.getSelectedRecipeKey();
    }

    @Override
    public boolean retainSelectionWhenOptionsEmpty() {
        return this.surface.retainSelectionWhenOptionsEmpty();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        this.surface.applyRemoteSelection(recipeKey);
        // Remote authoritative state must refresh the client preview too. The
        // original bridge only refreshed after server-side select/clear, which
        // let machine GUIs keep rendering their native first recipe.
        this.surface.refreshPreview();
    }

    @Override
    public SelectorPlacement getSelectorPlacement() {
        return this.surface.getSelectorPlacement();
    }

    @Override
    public SelectionScope getSelectionScope() {
        Object owner = this.surface.getRecipeOwner();
        return owner != null ? SelectionScope.shared(owner) : SelectionScope.local();
    }
}
