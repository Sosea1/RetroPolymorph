package dev.sosea1.retropolymorph.compat.gregtech;

import dev.sosea1.retropolymorph.core.SelectionServiceResult;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import dev.sosea1.retropolymorph.core.SelectionCommand;
import dev.sosea1.retropolymorph.core.SelectionService;
import dev.sosea1.retropolymorph.network.RecipeSelectionHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;
import org.apache.logging.log4j.LogManager;

/** Restores an explicit native-memory output through the normal selection service. */
public final class GregTechWorkbenchMemory {
    private GregTechWorkbenchMemory() { }

    @Nullable
    static SelectionServiceResult restore(GregTechWorkbenchContext context, World world,
            NBTTagCompound playerData, ItemStack desiredOutput) {
        if (desiredOutput == null || desiredOutput.isEmpty()) {
            return null;
        }
        List<RecipeOption> options = context.findOptions(world);
        String current = context.getSelectedRecipeKey();
        String choice = null;
        for (RecipeOption option : options) {
            if (ItemStack.areItemStacksEqual(desiredOutput, option.getOutput())) {
                if (choice == null) {
                    choice = option.getRecipeKey();
                }
                // Memory has no recipe ID: preserve a matching current recipe,
                // otherwise use the first matching option in registry order.
                if (option.getRecipeKey().equals(current)) {
                    return SelectionService.handle(world, playerData, context, SelectionCommand.select(current));
                }
            }
        }
        return choice != null
                ? SelectionService.handle(world, playerData, context, SelectionCommand.select(choice)) : null;
    }

    public static void onRecipeLoaded(Object widget, EntityPlayer player) {
        if (!PolymorphConfig.isIntegrationGregTechEnabled() || !(player instanceof EntityPlayerMP)
                || player.world == null || player.world.isRemote || player.openContainer == null) {
            return;
        }
        try {
            GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(player.openContainer);
            if (binding == null || binding.engine == null) {
                return;
            }
            ItemStack output = GregTechWorkbenchReflection.memoryResult(widget);
            GregTechWorkbenchContext context = new GregTechWorkbenchContext(player.openContainer, binding);
            SelectionServiceResult result = restore(context, player.world, player.getEntityData(), output);
            if (result != null && result.isAccepted()) {
                player.openContainer.detectAndSendChanges();
                RecipeSelectionHandler.syncExternalSelection((EntityPlayerMP) player, context, result);
                LogManager.getLogger("Retro Polymorph").debug(
                        "GT recipe memory restored: window={}, output={}, selected={}",
                        player.openContainer.windowId, output, result.getSelectedRecipeKey());
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            dev.sosea1.retropolymorph.compat.IntegrationHealthRegistry.recordBindingFailure(
                    "gregtech", "restoreRecipeMemory", exception);
            LogManager.getLogger("Retro Polymorph").warn("Could not restore GT recipe-memory selection", exception);
        }
    }
}
