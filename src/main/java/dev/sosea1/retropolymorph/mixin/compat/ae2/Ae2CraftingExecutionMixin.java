package dev.sosea1.retropolymorph.mixin.compat.ae2;

import appeng.helpers.InventoryAction;
import dev.sosea1.retropolymorph.compat.ae2.Ae2CraftExecutionScope;
import dev.sosea1.retropolymorph.compat.ae2.Ae2TerminalRecipePin;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carries the selected recipe through AE2's private scratch crafting matrix
 * during the actual CRAFT_ITEM / CRAFT_STACK / CRAFT_SHIFT execution path.
 */
@Pseudo
@Mixin(targets = "appeng.container.slot.SlotCraftingTerm", remap = false)
public abstract class Ae2CraftingExecutionMixin {

    @Unique
    private static final Logger retropolymorph$LOGGER = LogManager.getLogger("Retro Polymorph");

    @Unique
    private static final Set<String> retropolymorph$LOGGED_WIRELESS_RETURN_PINS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    @Inject(method = "doClick", at = @At("HEAD"), remap = false, require = 1)
    private void retropolymorph$beginCraftExecution(
            InventoryAction action,
            EntityPlayer player,
            CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationAe2Enabled()) {
            return;
        }
        Ae2CraftExecutionScope.enter(player);
        // AE2 UEL snapshots SlotCraftingTerm#getStack() as the requested craft
        // result before its later recipe lookup. Pin the actual server slot now,
        // while we still know the player/container-specific selection.
        Ae2TerminalRecipePin.pinForCraftClick(player, (Slot) (Object) this);
    }

    /**
     * Final guard for AE2 UEL's wireless/universal container.
     *
     * The item inserted into the player adaptor is the return value of
     * SlotCraftingTerm#craftItem, so for this wireless container we ensure
     * any non-empty stale return is replaced with the output captured at doClick HEAD.
     */
    @Inject(method = "craftItem", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void retropolymorph$pinWirelessCraftReturn(
            CallbackInfoReturnable<ItemStack> cir) {
        if (!PolymorphConfig.isIntegrationAe2Enabled()) {
            return;
        }
        Container container = Ae2CraftExecutionScope.currentContainer();
        if (container == null
                || !"appeng.container.implementations.ContainerWirelessCraftingTerminal"
                .equals(container.getClass().getName())) {
            return;
        }

        ItemStack before = cir.getReturnValue();
        ItemStack expected = Ae2CraftExecutionScope.currentExpectedOutput();
        if (before == null || before.isEmpty() || expected.isEmpty()) {
            return;
        }
        if (ItemStack.areItemStacksEqual(before, expected)) {
            return;
        }

        cir.setReturnValue(expected.copy());
        ResourceLocation beforeId = before.getItem().getRegistryName();
        ResourceLocation afterId = expected.getItem().getRegistryName();
        String key = container.getClass().getName()
                + "|" + String.valueOf(beforeId) + ":" + before.getMetadata()
                + "->" + String.valueOf(afterId) + ":" + expected.getMetadata();
        if (retropolymorph$LOGGED_WIRELESS_RETURN_PINS.add(key)) {
            retropolymorph$LOGGER.debug(
                    "AE2 wireless craft return pin active: container={}, before={}x{}, after={}x{}",
                    container.getClass().getName(),
                    beforeId,
                    Integer.valueOf(before.getCount()),
                    afterId,
                    Integer.valueOf(expected.getCount()));
        }
    }

    @Inject(method = "doClick", at = @At("RETURN"), remap = false, require = 1)
    private void retropolymorph$endCraftExecution(
            InventoryAction action,
            EntityPlayer player,
            CallbackInfo ci) {
        Ae2CraftExecutionScope.exit();
    }
}
