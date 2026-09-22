package dev.sosea1.retropolymorph.mixin.compat.ae2;

import dev.sosea1.retropolymorph.compat.ae2.Ae2TerminalRecipePin;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reasserts the selected result after terminal implementations run their own
 * onCraftMatrixChanged refresh and default back to the first recipe.
 */
@Pseudo
@Mixin(targets = {
        "appeng.container.implementations.ContainerWirelessCraftingTerminal",
        "p455w0rd.wct.container.ContainerWCT"
}, remap = false)
public abstract class Ae2TerminalRefreshPinMixin {

    @Inject(method = "func_75130_a", at = @At("RETURN"), remap = false, require = 0)
    private void retropolymorph$pinAfterMatrixRefresh(IInventory inventory, CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationAe2Enabled()) {
            return;
        }
        Ae2TerminalRecipePin.pinContainerResult((Container) (Object) this, "matrixRefresh");
    }
}
