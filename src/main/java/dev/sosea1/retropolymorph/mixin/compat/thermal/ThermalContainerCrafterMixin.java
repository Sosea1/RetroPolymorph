package dev.sosea1.retropolymorph.mixin.compat.thermal;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.client.ClientSelectionTracker;
import dev.sosea1.retropolymorph.compat.thermal.ThermalSequentialFabricatorPacketSelectionBridge;
import dev.sosea1.retropolymorph.compat.thermal.ThermalSequentialFabricatorSelectionStore;
import dev.sosea1.retropolymorph.compat.thermal.ThermalSequentialFabricatorRecipeSupport;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.RecipeProbe;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores the machine-level selected recipe into the initial server preview. */
@Pseudo
@Mixin(targets = "cofh.thermalexpansion.gui.container.machine.ContainerCrafter", remap = false)
public abstract class ThermalContainerCrafterMixin {

    /**
     * Thermal commits its ghost grid only when the user presses Set Recipe.
     * Stage the currently server-authoritative selector choice on the client
     * tile so TileCrafter#getModePacket can append it to the same native packet.
     */
    @Inject(method = "setRecipe", at = @At("HEAD"), remap = false, require = 1)
    private void retropolymorph$stageSelectionForModePacket(CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationThermalEnabled()) {
            return;
        }
        Container container = (Container) (Object) this;
        TileEntity tile = null;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof TileEntity
                    && "cofh.thermalexpansion.block.machine.TileCrafter"
                    .equals(slot.inventory.getClass().getName())) {
                tile = (TileEntity) slot.inventory;
                break;
            }
        }
        if (tile == null || tile.getWorld() == null || !tile.getWorld().isRemote
                || !(tile instanceof ThermalSequentialFabricatorPacketSelectionBridge)) {
            return;
        }

        if (!ClientSelectionTracker.hasAuthoritativeSnapshotForWindow(container.windowId)) {
            // If Set Recipe is clicked before the first server snapshot arrives,
            // do not append a fake "clear". The server can safely reuse the
            // previously committed machine choice and still revalidate it
            // against Thermal's incoming 3x3 definition.
            return;
        }
        String key = ClientSelectionTracker.getSelectedRecipeKeyForWindow(container.windowId);
        ResourceLocation selected = RecipeKey.parseForgeId(key);
        ((ThermalSequentialFabricatorPacketSelectionBridge) tile)
                .retropolymorph$stageRecipeSelection(selected);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$restoreSelectedPreview(
            InventoryPlayer inventory,
            TileEntity tile,
            CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationThermalEnabled()
                || tile == null
                || tile.getWorld() == null
                || tile.getWorld().isRemote) {
            return;
        }

        Container container = (Container) (Object) this;
        InventoryCrafting matrix = null;
        Slot resultSlot = null;
        for (Slot slot : container.inventorySlots) {
            if (matrix == null
                    && slot.inventory instanceof InventoryCrafting
                    && slot.inventory.getSizeInventory() == 9) {
                matrix = (InventoryCrafting) slot.inventory;
            }
            if (slot.xPos == 125 && slot.yPos == 48
                    && slot.inventory instanceof InventoryCraftResult) {
                resultSlot = slot;
            }
        }
        if (matrix == null || resultSlot == null) {
            return;
        }

        ResourceLocation selected = ThermalSequentialFabricatorSelectionStore
                .getSelectedRecipeId(tile);
        if (selected == null) {
            return;
        }
        IRecipe recipe = net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES
                .getValue(selected);
        if (recipe == null
                || !ThermalSequentialFabricatorRecipeSupport.isSupported(recipe)
                || !RecipeProbe.matches(recipe, matrix, tile.getWorld())) {
            return;
        }

        RecipeSelectionSeeder.seed(matrix, selected);
        InventoryCraftResult result = (InventoryCraftResult) resultSlot.inventory;
        result.setRecipeUsed(recipe);
        ItemStack output = RecipeProbe.craftingResult(recipe, matrix);
        result.setInventorySlotContents(
                0,
                output == null || output.isEmpty() ? ItemStack.EMPTY : output.copy());
    }
}
