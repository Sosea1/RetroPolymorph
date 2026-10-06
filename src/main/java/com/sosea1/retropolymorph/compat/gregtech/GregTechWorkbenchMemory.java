package com.sosea1.retropolymorph.compat.gregtech;

import com.sosea1.retropolymorph.core.SelectionServiceResult;
import com.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import com.sosea1.retropolymorph.api.RecipeOption;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import com.sosea1.retropolymorph.core.SelectionCommand;
import com.sosea1.retropolymorph.core.SelectionService;
import com.sosea1.retropolymorph.network.RecipeSelectionHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.sosea1.retropolymorph.compat.gregtech.GregTechWorkbenchReflection.findMethod;
import static com.sosea1.retropolymorph.compat.gregtech.GregTechWorkbenchReflection.invoke;
import static com.sosea1.retropolymorph.compat.gregtech.GregTechWorkbenchReflection.readField;

/** Restores an explicit native-memory output through the normal selection service. */
public final class GregTechWorkbenchMemory {
    private static final Logger LOGGER = LogManager.getLogger("Retro Polymorph");

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
                LOGGER.debug(
                        "GT recipe memory restored: window={}, output={}, selected={}",
                        player.openContainer.windowId, output, result.getSelectedRecipeKey());
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "gregtech", "restoreRecipeMemory", exception);
            LOGGER.warn("Could not restore GT recipe-memory selection", exception);
        }
    }

    public static void onRecipeLoadedCeu(Object memory, int index) {
        if (!PolymorphConfig.isIntegrationGregTechEnabled() || memory == null) {
            return;
        }
        try {
            Object syncManager = invoke(memory, "getSyncManager");
            if (syncManager == null) {
                return;
            }
            if (Boolean.TRUE.equals(invoke(syncManager, "isClient"))) {
                return;
            }
            Object playerObj = invoke(syncManager, "getPlayer");
            if (!(playerObj instanceof EntityPlayerMP)) {
                return;
            }
            EntityPlayerMP player = (EntityPlayerMP) playerObj;
            if (player.world == null || player.openContainer == null) {
                return;
            }

            // Memory changes the entire ghost grid; send it before the selector reply.
            Object logic = invoke(memory, "getRecipeLogic");
            Object slots = logic == null ? null : readField(logic, "inputSlots");
            if (slots instanceof Object[]) {
                for (Object slot : (Object[]) slots) {
                    Object handler = slot == null ? null : readField(slot, "syncHandler");
                    if (handler == null) {
                        continue;
                    }
                    Method detect = findMethod(handler.getClass(), "detectAndSendChanges", boolean.class);
                    if (detect != null) {
                        detect.invoke(handler, Boolean.TRUE);
                    }
                }
            }

            Method getOutput = findMethod(memory.getClass(), "getRecipeOutputAtIndex", int.class);
            if (getOutput == null) {
                return;
            }
            Object outputObj = getOutput.invoke(memory, Integer.valueOf(index));
            if (!(outputObj instanceof ItemStack)) {
                return;
            }
            ItemStack output = (ItemStack) outputObj;
            if (output.isEmpty()) {
                return;
            }

            GregTechWorkbenchReflection.Binding binding = GregTechWorkbenchReflection.bind(player.openContainer);
            if (binding == null || binding.engine == null) {
                return;
            }
            GregTechWorkbenchContext context = new GregTechWorkbenchContext(player.openContainer, binding);
            SelectionServiceResult result = restore(context, player.world, player.getEntityData(), output);
            if (result != null && result.isAccepted()) {
                player.openContainer.detectAndSendChanges();
                RecipeSelectionHandler.syncExternalSelection(player, context, result);
                LOGGER.debug(
                        "GTCEu recipe memory loaded: window={}, index={}, output={}, selected={}",
                        player.openContainer.windowId, Integer.valueOf(index), output, result.getSelectedRecipeKey());
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            IntegrationHealthRegistry.recordBindingFailure(
                    "gregtech", "restoreRecipeMemoryCeu", exception);
            LOGGER.warn("Could not restore GTCEu recipe-memory selection", exception);
        }
    }

}
