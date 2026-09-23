package dev.sosea1.retropolymorph.compat.ae2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Soft bridge mixed into AE2 UEL's ContainerCraftingTerm when AE2 is present.
 */
public interface Ae2CraftingTermExtension {

    @Nullable
    IRecipe retropolymorph$getAe2CurrentRecipe();

    void retropolymorph$setAe2CurrentRecipe(@Nullable IRecipe recipe);

    @Nullable
    ResourceLocation retropolymorph$getAe2SelectedRecipeId();

    void retropolymorph$setAe2SelectedRecipeId(@Nullable ResourceLocation recipeId);

    /**
     * Returns the player viewing this terminal.
     *
     * <p>AE2's {@code AppEngSlot} always passes {@code emptyInventory} to the Slot
     * superclass, so {@code slot.inventory instanceof InventoryPlayer} is always false
     * for real AE2 containers. This bridge method uses {@code AEBaseContainer#getPlayerInv()}
     * — the only reliable API — to expose the player.</p>
     */
    @Nullable
    EntityPlayer retropolymorph$getAe2Player();
}
