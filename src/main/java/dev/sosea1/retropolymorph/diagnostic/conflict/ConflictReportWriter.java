package dev.sosea1.retropolymorph.diagnostic.conflict;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/** Deterministic UTF-8 text output for /retropolymorph conflicts. */
public final class ConflictReportWriter {

    private ConflictReportWriter() {
    }

    public static File write(File logsDirectory, ConflictReport report) throws IOException {
        if (!logsDirectory.isDirectory()
                && !logsDirectory.mkdirs()
                && !logsDirectory.isDirectory()) {
            throw new IOException("Could not create log directory: " + logsDirectory);
        }

        File file = new File(logsDirectory, "retropolymorph-conflicts.txt");
        Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8));
        try {
            writeReport(writer, report);
        } finally {
            writer.close();
        }
        return file;
    }

    static void writeReport(Writer writer, ConflictReport report) throws IOException {
        writer.write("Retro Polymorph conflict diagnostics\n");
        writer.write("================================\n\n");
        writer.write("Crafting recipes scanned: " + report.getCraftingRecipesScanned() + "\n");
        writer.write("Crafting signature candidates: "
                + report.getCraftingCandidates().size() + "\n\n");
        writer.write("Crafting candidates are best-effort: arbitrary IRecipe implementations may\n");
        writer.write("apply conditions that are not represented by getIngredients().\n\n");

        int index = 1;
        for (CraftingConflictGroup group : report.getCraftingCandidates()) {
            writer.write("[crafting " + index++ + "] " + group.getInputDescription() + "\n");
            for (String recipe : group.getRecipes()) {
                writer.write("  - " + recipe + "\n");
            }
            writer.write("\n");
        }

        writer.write("Furnace registrations scanned: " + report.getFurnaceRecipesScanned() + "\n");
        writer.write("Furnace conflicts: " + report.getFurnaceConflicts().size() + "\n\n");
        index = 1;
        for (FurnaceConflictPair pair : report.getFurnaceConflicts()) {
            writer.write("[furnace " + index++ + "] " + pair.getInputDescription() + "\n");
            writer.write("  - " + pair.getFirst() + "\n");
            writer.write("  - " + pair.getSecond() + "\n\n");
        }
    }
}
