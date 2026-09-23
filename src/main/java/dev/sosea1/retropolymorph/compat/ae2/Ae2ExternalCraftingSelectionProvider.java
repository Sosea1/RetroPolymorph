package dev.sosea1.retropolymorph.compat.ae2;

import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProvider;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Supplies external selection state from AE2 terminal containers and craft execution scopes.
 */
public final class Ae2ExternalCraftingSelectionProvider implements ExternalCraftingSelectionProvider {

    public static final Ae2ExternalCraftingSelectionProvider INSTANCE = new Ae2ExternalCraftingSelectionProvider();
    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

    private static final Set<String> LOGGED_AE2_OWNER_BRIDGES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> LOGGED_AE2_SCOPED_BRIDGES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private Ae2ExternalCraftingSelectionProvider() {
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.AUTHORITATIVE;
    }

    @Nullable
    @Override
    public ResourceLocation getSelectedRecipeId(InventoryCrafting matrix, @Nullable Container owner) {
        ResourceLocation selected = owner == null ? null : Ae2SelectionStore.get(owner);
        if (selected == null && owner instanceof Ae2CraftingTermExtension) {
            selected = ((Ae2CraftingTermExtension) owner).retropolymorph$getAe2SelectedRecipeId();
            if (selected != null) {
                Ae2SelectionStore.set(owner, selected);
            }
        }

        if (selected == null && matrix.getSizeInventory() == 9) {
            Container active = Ae2MatrixChangeScope.currentContainer();
            if (active != null) {
                selected = Ae2SelectionStore.get(active);
                if (selected == null && active instanceof Ae2CraftingTermExtension) {
                    selected = ((Ae2CraftingTermExtension) active).retropolymorph$getAe2SelectedRecipeId();
                    if (selected != null) {
                        Ae2SelectionStore.set(active, selected);
                    }
                }
            }
        }

        if (selected == null && Ae2CraftExecutionScope.isActive() && matrix.getSizeInventory() == 9) {
            selected = Ae2CraftExecutionScope.currentSelectedRecipeId();
            if (selected != null) {
                String ownerClass = owner == null ? "<null>" : owner.getClass().getName();
                String logKey = ownerClass + "|" + selected;
                if (LOGGED_AE2_SCOPED_BRIDGES.add(logKey)) {
                    LOGGER.debug(
                            "AE2 scoped CraftingManager bridge active: owner={}, selected={}",
                            ownerClass,
                            selected);
                }
            }
        }

        if (selected != null && owner != null) {
            String ownerClass = owner.getClass().getName();
            if (LOGGED_AE2_OWNER_BRIDGES.add(ownerClass)) {
                LOGGER.debug(
                        "AE2 CraftingManager owner bridge active: owner={}, selected={}",
                        ownerClass,
                        selected);
            }
        }

        return selected;
    }

    @Override
    public void onRecipeOutputResolved(InventoryCrafting matrix, @Nullable Container owner, IRecipe recipe) {
        Ae2TerminalRecipePin.pinOwnerResult(matrix, recipe);
    }

    @Override
    public boolean shouldClearStateOnEmpty(InventoryCrafting matrix, @Nullable Container owner) {
        return owner instanceof Ae2CraftingTermExtension;
    }
}
