package dev.sosea1.retropolymorph.command;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import dev.sosea1.retropolymorph.diagnostic.CraftingDetectionDiagnostics;
import dev.sosea1.retropolymorph.diagnostic.conflict.ConflictReport;
import dev.sosea1.retropolymorph.diagnostic.conflict.ConflictReportWriter;
import dev.sosea1.retropolymorph.diagnostic.conflict.ConflictScanner;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Temporary alpha test surface for validating generic and focused recipe
 * selection contexts against real modded containers.
 */
public final class CommandRetroPolymorph extends CommandBase {

    @Override
    public String getName() {
        return "retropolymorph";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/retropolymorph <conflicts|diagnose|list|select <recipe_key>|clear>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && "conflicts".equalsIgnoreCase(args[0])) {
            scanConflicts(server, sender);
            return;
        }

        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("retropolymorph.command.player_only");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        if (args.length == 1 && "diagnose".equalsIgnoreCase(args[0])) {
            diagnose(player);
            return;
        }

        SelectionContext context = SelectionContextDetector.detect(player.openContainer);
        if (context == null) {
            throw new CommandException("No compatible recipe-selection context detected in the open container");
        }

        if (args.length == 0 || (args.length == 1 && "list".equalsIgnoreCase(args[0]))) {
            listMatches(player, context);
            return;
        }

        if (args.length == 1 && "clear".equalsIgnoreCase(args[0])) {
            context.clearSelection();
            player.sendMessage(new TextComponentString("[Retro Polymorph] selection cleared"));
            return;
        }

        if (args.length == 2 && "select".equalsIgnoreCase(args[0])) {
            String recipeKey = args[1];
            if (!context.select(recipeKey, player.world)) {
                throw new CommandException(
                        "Recipe key does not exist or does not match the current matrix: " + recipeKey);
            }

            player.sendMessage(new TextComponentString("[Retro Polymorph] selected " + recipeKey));
            return;
        }

        throw new CommandException(getUsage(sender));
    }


    private static void scanConflicts(MinecraftServer server, ICommandSender sender)
            throws CommandException {
        ConflictReport report = ConflictScanner.scan();
        File file;
        try {
            file = ConflictReportWriter.write(server.getFile("logs"), report);
        } catch (IOException e) {
            throw new CommandException(
                    "Failed to write Retro Polymorph conflict report: " + e.getMessage());
        }

        sender.sendMessage(new TextComponentString(
                "[Retro Polymorph] crafting candidates=" + report.getCraftingCandidates().size()
                        + ", furnace conflicts=" + report.getFurnaceConflicts().size()));
        sender.sendMessage(new TextComponentString(
                "[Retro Polymorph] report: " + file.getPath()));
    }

    private static void diagnose(EntityPlayerMP player) {
        SelectionContext selection = SelectionContextDetector.detect(player.openContainer);
        String containerClass = player.openContainer == null
                ? "<null>"
                : player.openContainer.getClass().getName();
        player.sendMessage(new TextComponentString(
                "[Retro Polymorph] container: " + containerClass));

        if (selection != null) {
            String selected = selection.getSelectedRecipeKey();
            player.sendMessage(new TextComponentString(
                    "  context: " + selection.getClass().getName()));
            player.sendMessage(new TextComponentString(
                    "  inputs=" + selection.getInputCount()
                            + ", resultSlot=" + selection.getResultSlot().getClass().getName()));
            player.sendMessage(new TextComponentString(
                    "  selected: " + (selected == null ? "<default>" : selected)));
        } else {
            player.sendMessage(new TextComponentString(
                    "  context: <unsupported or ambiguous>"));
        }

        if (selection instanceof RecipeSelectionContext || selection == null) {
            diagnoseCraftingDetails(player);
        }
    }

    private static void diagnoseCraftingDetails(EntityPlayerMP player) {
        CraftingDetectionDiagnostics.Report report =
                CraftingDetectionDiagnostics.inspect(player.openContainer);
        player.sendMessage(new TextComponentString(
                "  craftingRoute: " + report.route + " | " + report.detail));
        player.sendMessage(new TextComponentString(
                "  slots=" + report.totalSlots
                        + ", slotCrafting=" + report.slotCraftingCount
                        + ", accessors=" + report.slotCraftingAccessorCount));
        player.sendMessage(new TextComponentString(
                "  craftingMatrices=" + report.craftingMatrixCount
                        + ", resultInventories=" + report.resultInventoryCount));
    }

    private static void listMatches(EntityPlayerMP player, SelectionContext context) {
        List<RecipeOption> options = context.findOptions(player.world);
        player.sendMessage(new TextComponentString(
                "[Retro Polymorph] selectable recipes: " + options.size()));

        int shown = Math.min(options.size(), 20);
        for (int i = 0; i < shown; i++) {
            RecipeOption option = options.get(i);
            ItemStack output = option.getOutput();
            String outputName = output.isEmpty() ? "<empty>" : output.getDisplayName();
            player.sendMessage(new TextComponentString(
                    "  " + i + ": " + option.getRecipeKey() + " -> " + outputName));
        }

        if (options.size() > shown) {
            player.sendMessage(new TextComponentString(
                    "  ... and " + (options.size() - shown) + " more"));
        }
    }
}
