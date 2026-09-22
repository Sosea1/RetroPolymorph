package dev.sosea1.retropolymorph.mixin.compat.thermal;

import cofh.core.network.PacketBase;
import dev.sosea1.retropolymorph.compat.thermal.ThermalSequentialFabricatorPacketPayload;
import dev.sosea1.retropolymorph.compat.thermal.ThermalSequentialFabricatorPacketSelectionBridge;
import dev.sosea1.retropolymorph.compat.thermal.ThermalSequentialFabricatorRecipeSupport;
import dev.sosea1.retropolymorph.compat.thermal.ThermalSequentialFabricatorSelectionStore;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.CraftingMatrixExtension;
import dev.sosea1.retropolymorph.core.RecipeSelectionSeeder;
import dev.sosea1.retropolymorph.core.RecipeSelectionState;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/** Carries the GUI-selected recipe through Thermal's native Set Recipe packet. */
@Pseudo
@Mixin(targets = "cofh.thermalexpansion.block.machine.TileCrafter", remap = false)
public abstract class ThermalTileCrafterMixin
        implements ThermalSequentialFabricatorPacketSelectionBridge {

    @Unique
    private boolean retropolymorph$stagedSelectionPresent;

    @Unique
    @Nullable
    private ResourceLocation retropolymorph$stagedSelection;

    @Override
    public void retropolymorph$stageRecipeSelection(@Nullable ResourceLocation recipeId) {
        this.retropolymorph$stagedSelectionPresent = true;
        this.retropolymorph$stagedSelection = recipeId;
    }

    @Override
    public boolean retropolymorph$hasStagedRecipeSelection() {
        return this.retropolymorph$stagedSelectionPresent;
    }

    @Override
    @Nullable
    public ResourceLocation retropolymorph$getStagedRecipeSelection() {
        return this.retropolymorph$stagedSelection;
    }

    @Override
    public void retropolymorph$clearStagedRecipeSelection() {
        this.retropolymorph$stagedSelectionPresent = false;
        this.retropolymorph$stagedSelection = null;
    }

    /**
     * ContainerCrafter stages the current server-authoritative choice on the
     * client tile immediately before Thermal builds its normal mode packet.
     * Appending one UTF string keeps recipe identity and the 3x3 definition in
     * the same native packet/transaction instead of racing two network channels.
     */
    @Inject(method = "getModePacket", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$appendSelectedRecipe(
            CallbackInfoReturnable<PacketBase> cir) {
        if (!PolymorphConfig.isIntegrationThermalEnabled()
                || !this.retropolymorph$stagedSelectionPresent) {
            return;
        }
        PacketBase payload = cir.getReturnValue();
        if (payload != null) {
            ResourceLocation selected = this.retropolymorph$stagedSelection;
            payload.addString(ThermalSequentialFabricatorPacketPayload.encode(selected));
        }
        // sendModePacket/getModePacket is synchronous on the client. Do not let
        // a later unrelated mode packet accidentally reuse this GUI selection.
        retropolymorph$clearStagedRecipeSelection();
    }

    /** Read our trailing field after Thermal consumed its nine recipe stacks. */
    @Inject(
            method = "handleModePacket",
            at = @At(
                    value = "INVOKE",
                    target = "Lcofh/thermalexpansion/block/machine/TileCrafter;setRecipe()V",
                    shift = At.Shift.BEFORE),
            remap = false,
            require = 1)
    private void retropolymorph$readSelectedRecipe(
            PacketBase payload,
            CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationThermalEnabled()
                || payload == null
                || payload.datain == null) {
            return;
        }
        ThermalSequentialFabricatorPacketPayload.Decoded decoded =
                ThermalSequentialFabricatorPacketPayload.readOptional(payload.datain);
        if (decoded.isRecognized()) {
            retropolymorph$stageRecipeSelection(decoded.getRecipeId());
        }
    }

    @Inject(method = "handleModePacket", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$clearPacketSelection(PacketBase payload, CallbackInfo ci) {
        retropolymorph$clearStagedRecipeSelection();
    }

    @Redirect(
            method = "setRecipe",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/CraftingManager;"
                            + "func_192413_b(Lnet/minecraft/inventory/InventoryCrafting;"
                            + "Lnet/minecraft/world/World;)Lnet/minecraft/item/crafting/IRecipe;",
                    remap = false),
            remap = false,
            require = 1)
    private IRecipe retropolymorph$resolveSelectedRecipe(
            InventoryCrafting matrix,
            World world) {
        TileEntity tile = (TileEntity) (Object) this;
        if (!PolymorphConfig.isIntegrationThermalEnabled()) {
            return CraftingManager.findMatchingRecipe(matrix, world);
        }

        boolean packetCommit = this.retropolymorph$stagedSelectionPresent;
        ResourceLocation selected = packetCommit
                ? this.retropolymorph$stagedSelection
                : ThermalSequentialFabricatorSelectionStore.getSelectedRecipeId(tile);

        if (selected != null) {
            IRecipe candidate = ForgeRegistries.RECIPES.getValue(selected);
            if (!ThermalSequentialFabricatorRecipeSupport.isSupported(candidate)) {
                selected = null;
            }
        }

        if (selected == null) {
            retropolymorph$clearMatrixSelection(matrix);
        } else {
            RecipeSelectionSeeder.seed(matrix, selected);
        }

        IRecipe resolved = CraftingManager.findMatchingRecipe(matrix, world);
        ResourceLocation resolvedId = resolved == null ? null : resolved.getRegistryName();

        if (packetCommit) {
            // This is the native Set Recipe transaction. Persist only a recipe
            // that survived Thermal's incoming 3x3 definition and Forge matches().
            ThermalSequentialFabricatorSelectionStore.writeSelectedRecipeId(
                    tile,
                    selected != null && selected.equals(resolvedId) ? selected : null);
        } else if (selected != null && !selected.equals(resolvedId)) {
            // A persisted choice no longer matches the committed machine grid.
            ThermalSequentialFabricatorSelectionStore.writeSelectedRecipeId(tile, null);
        }
        return resolved;
    }

    @Unique
    private static void retropolymorph$clearMatrixSelection(InventoryCrafting matrix) {
        if (!(matrix instanceof CraftingMatrixExtension)) {
            return;
        }
        RecipeSelectionState state = ((CraftingMatrixExtension) matrix)
                .retropolymorph$peekRecipeSelectionState();
        if (state != null) {
            state.clear();
        }
    }
}
