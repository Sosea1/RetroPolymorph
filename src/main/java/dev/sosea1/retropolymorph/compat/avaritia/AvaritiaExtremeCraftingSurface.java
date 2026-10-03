package dev.sosea1.retropolymorph.compat.avaritia;

import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeOption;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

final class AvaritiaExtremeCraftingSurface implements MachineRecipeSurface {

    private final Container container;
    private final AvaritiaExtremeCraftingReflection.Binding binding;

    AvaritiaExtremeCraftingSurface(
            Container container,
            AvaritiaExtremeCraftingReflection.Binding binding) {
        this.container = container;
        this.binding = binding;
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public Object getRecipeOwner() {
        return this.binding.owner;
    }

    @Override
    public int getInputCount() {
        return this.binding.matrix.getSizeInventory();
    }

    @Override
    public ItemStack getInputStack(int index) {
        return index >= 0 && index < this.binding.matrix.getSizeInventory()
                ? this.binding.matrix.getStackInSlot(index)
                : ItemStack.EMPTY;
    }

    @Override
    public int getClientStateToken() {
        String selected = AvaritiaExtremeCraftingReflection.getSelectedKey(this.binding.owner);
        return selected == null ? 0 : selected.hashCode();
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        return world == null
                ? Collections.<RecipeOption>emptyList()
                : AvaritiaExtremeCraftingReflection.findOptions(this.binding, world);
    }

    @Override
    public boolean selectRecipe(String recipeKey, World world) {
        return world != null
                && AvaritiaExtremeCraftingReflection.select(this.binding, recipeKey, world);
    }

    @Override
    public void clearRecipeSelection() {
        AvaritiaExtremeCraftingReflection.clear(this.binding.owner);
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        return AvaritiaExtremeCraftingReflection.getSelectedKey(this.binding.owner);
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        AvaritiaExtremeCraftingReflection.setSelectedKey(this.binding.owner, recipeKey);
    }

    @Override
    public void refreshPreview() {
        this.container.onCraftMatrixChanged(this.binding.matrix);
    }

    @Override
    public MachineRecipePersistence getPersistencePolicy() {
        return MachineRecipePersistence.OWNER_WITH_PLAYER_PROFILE;
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
