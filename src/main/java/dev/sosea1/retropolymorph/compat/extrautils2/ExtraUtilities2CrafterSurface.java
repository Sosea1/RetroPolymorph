package dev.sosea1.retropolymorph.compat.extrautils2;

import dev.sosea1.retropolymorph.api.MachineRecipePersistence;
import dev.sosea1.retropolymorph.api.MachineRecipeSurface;
import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectorPlacement;
import dev.sosea1.retropolymorph.core.CraftingRecipeOptionCollector;
import dev.sosea1.retropolymorph.core.CraftingRules;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Machine-API surface over one XU2 Analog/Mechanical Crafter recipe grid. */
final class ExtraUtilities2CrafterSurface implements MachineRecipeSurface {

    private static final Container MIRROR_OWNER = new MirrorContainer();
    // XU2 places the visible result slot 47 px to the right of the rightmost
    // 3x3 input-slot origin. Anchor to that actual result column so the
    // selector is centered above the output instead of above the arrow.
    private static final int RESULT_X_FROM_GRID_RIGHT = 47;

    private final Container container;
    private final TileEntity tile;
    private final ExtraUtilities2SelectionAccess selection;
    private final Slot[] inputSlots;
    private final int anchorX;
    private final int anchorY;
    private final InventoryCrafting matrix;

    ExtraUtilities2CrafterSurface(Container container, ExtraUtilities2CrafterReflection.Resolved resolved) {
        this.container = container;
        this.tile = resolved.tile;
        this.selection = (ExtraUtilities2SelectionAccess) resolved.tile;
        this.inputSlots = resolved.inputSlots;
        this.matrix = new InventoryCrafting(MIRROR_OWNER, 3, 3);
        // XU2 has no trustworthy result Slot, but its DynamicGui keeps the
        // 3x3 -> arrow -> result geometry stable. Use the real result column as
        // a virtual anchor rather than the arrow gap used by earlier batches.
        this.anchorX = resolved.maxX + RESULT_X_FROM_GRID_RIGHT;
        this.anchorY = (resolved.minY + resolved.maxY) / 2;
        refreshMatrix();
    }

    @Override
    public Container getContainer() {
        return this.container;
    }

    @Override
    public Object getRecipeOwner() {
        return this.tile;
    }

    @Override
    public int getInputCount() {
        return 9;
    }

    @Override
    public ItemStack getInputStack(int index) {
        return index >= 0 && index < this.inputSlots.length
                ? this.inputSlots[index].getStack()
                : ItemStack.EMPTY;
    }

    @Override
    public int getClientStateToken() {
        return this.tile.getClass().getName().hashCode();
    }

    @Override
    public List<RecipeOption> findOptions(World world) {
        refreshMatrix();
        if (CraftingRules.isRepairCombination(this.matrix)) {
            return Collections.emptyList();
        }
        return CraftingRecipeOptionCollector.collectForge(
                this.matrix,
                RecipeResolver.findAllMatches(this.matrix, world));
    }

    @Override
    public boolean selectRecipe(String recipeKey, World world) {
        ResourceLocation id = RecipeKey.parseForgeId(recipeKey);
        if (id == null) {
            return false;
        }
        refreshMatrix();
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(id);
        if (recipe == null || !RecipeProbe.matches(recipe, this.matrix, world)) {
            return false;
        }
        this.selection.retropolymorph$setExtraUtilities2Recipe(id);
        return true;
    }

    @Override
    public void clearRecipeSelection() {
        this.selection.retropolymorph$setExtraUtilities2Recipe(null);
    }

    @Override
    @Nullable
    public String getSelectedRecipeKey() {
        ResourceLocation id = this.selection.retropolymorph$getExtraUtilities2Recipe();
        return id == null ? null : id.toString();
    }

    @Override
    public void applyRemoteSelection(@Nullable String recipeKey) {
        this.selection.retropolymorph$setExtraUtilities2Recipe(RecipeKey.parseForgeId(recipeKey));
    }

    @Override
    public void refreshPreview() {
        // The tile mixins invalidate XU2's own curRecipe cache. Do not feed the
        // private mirror back into DynamicContainer: XU2 does not own that
        // InventoryCrafting and its real preview/operation is rebuilt by the
        // next native preOperate()/getRecipe() pass.
        this.container.detectAndSendChanges();
    }

    @Override
    public MachineRecipePersistence getPersistencePolicy() {
        // The recipe must remain tile-owned for autonomous crafting, but XU2
        // should still honour the same conflict choice the player already made
        // in a normal table/terminal. The hybrid policy does exactly that.
        return MachineRecipePersistence.OWNER_WITH_PLAYER_PROFILE;
    }

    @Override
    public boolean controlsActualOperation() {
        // The XU2 mixins reorder the exact CraftingHelper112 native scan and invalidate curRecipe.
        return true;
    }

    @Override
    @Nullable
    public Slot getResultSlot() {
        // XU2 exposes no trustworthy InventoryCraftResult-backed output slot;
        // the machine surface API uses virtual coordinate anchors instead.
        return null;
    }

    @Override
    public SelectorPlacement getSelectorPlacement() {
        return SelectorPlacement.resultSlot(this.anchorX, this.anchorY, 0, 0);
    }

    private void refreshMatrix() {
        for (int i = 0; i < 9; i++) {
            ItemStack source = this.inputSlots[i].getStack();
            ItemStack mirrored = this.matrix.getStackInSlot(i);
            if (mirrored != source && !ItemStack.areItemStacksEqual(mirrored, source)) {
                this.matrix.setInventorySlotContents(
                        i,
                        source.isEmpty() ? ItemStack.EMPTY : source.copy());
            }
        }
    }

    private static final class MirrorContainer extends Container {
        @Override
        public void onCraftMatrixChanged(IInventory inventory) {
            // Mirror updates are local bookkeeping; refreshPreview invokes the
            // real XU2 container exactly once after the mirror is complete.
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }
}
