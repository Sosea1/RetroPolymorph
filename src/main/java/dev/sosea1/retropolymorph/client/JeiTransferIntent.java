package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import dev.sosea1.retropolymorph.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Short-lived client intent captured from a successful JEI recipe transfer.
 *
 * It is bound to the actual Container object, not to a GUI session token,
 * because JEI temporarily replaces the GuiContainer with its RecipesGui.
 */
public final class JeiTransferIntent {

    private static final long MAX_AGE_NANOS = 10_000_000_000L;
    private static final int MAX_OUTPUT_VARIANTS = 32;

    private static Container targetContainer;
    private static ItemStack[] beforeInputs = new ItemStack[0];
    private static List<ItemStack> intendedOutputs = Collections.emptyList();
    private static boolean safeWithoutInputChange;
    private static long createdAtNanos;

    private JeiTransferIntent() {
    }

    public static void capture(Container container, List<ItemStack> outputs) {
        if (container == null || outputs == null || outputs.isEmpty()) {
            clear();
            return;
        }

        ArrayList<ItemStack> unique = new ArrayList<ItemStack>();
        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty() || containsStack(unique, output)) {
                continue;
            }
            unique.add(output.copy());
            if (unique.size() >= MAX_OUTPUT_VARIANTS) {
                break;
            }
        }
        if (unique.isEmpty()) {
            clear();
            return;
        }

        SelectionContext context = SelectionContextDetector.detect(container);
        targetContainer = container;
        intendedOutputs = Collections.unmodifiableList(unique);
        createdAtNanos = System.nanoTime();
        safeWithoutInputChange = false;

        if (context == null) {
            beforeInputs = new ItemStack[0];
            return;
        }

        beforeInputs = snapshot(context);
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world != null) {
            List<RecipeOption> choices = ClientRecipeCache.sanitizeOptions(
                    context.findOptions(world));
            Match match = findMatch(choices, intendedOutputs);
            safeWithoutInputChange = match.option != null && !match.ambiguous;
        }
    }

    static boolean tryApply(
            SelectionContext context,
            List<RecipeOption> choices,
            int sessionToken) {
        if (targetContainer == null) {
            return false;
        }
        if (isExpired() || context.getContainer() != targetContainer) {
            clear();
            return false;
        }

        boolean inputChanged = !snapshotMatches(context, beforeInputs);
        if (!safeWithoutInputChange && !inputChanged) {
            return false;
        }

        Match match = findMatch(choices, intendedOutputs);
        clear();
        if (match.ambiguous || match.option == null) {
            return false;
        }

        NetworkHandler.select(
                context.getContainer().windowId,
                sessionToken,
                match.option.getRecipeKey());
        return true;
    }

    static void discardIfDifferent(Container container) {
        if (targetContainer != null && targetContainer != container) {
            clear();
        }
    }

    static void clear() {
        targetContainer = null;
        beforeInputs = new ItemStack[0];
        intendedOutputs = Collections.emptyList();
        safeWithoutInputChange = false;
        createdAtNanos = 0L;
    }

    private static boolean isExpired() {
        return createdAtNanos == 0L
                || System.nanoTime() - createdAtNanos > MAX_AGE_NANOS;
    }

    private static ItemStack[] snapshot(SelectionContext context) {
        ItemStack[] result = new ItemStack[context.getInputCount()];
        for (int index = 0; index < result.length; index++) {
            ItemStack stack = context.getInputStack(index);
            result[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
        return result;
    }

    private static boolean snapshotMatches(SelectionContext context, ItemStack[] snapshot) {
        if (context.getInputCount() != snapshot.length) {
            return false;
        }
        for (int index = 0; index < snapshot.length; index++) {
            if (!ItemStack.areItemStacksEqual(snapshot[index], context.getInputStack(index))) {
                return false;
            }
        }
        return true;
    }

    private static Match findMatch(List<RecipeOption> choices, List<ItemStack> outputs) {
        RecipeOption found = null;
        for (RecipeOption choice : choices) {
            if (!containsStack(outputs, choice.getOutput())) {
                continue;
            }
            if (found != null && !found.getRecipeKey().equals(choice.getRecipeKey())) {
                return Match.AMBIGUOUS;
            }
            found = choice;
        }
        return found == null ? Match.NONE : new Match(found, false);
    }

    private static boolean containsStack(List<ItemStack> stacks, ItemStack target) {
        for (ItemStack stack : stacks) {
            if (ItemStack.areItemStacksEqual(stack, target)) {
                return true;
            }
        }
        return false;
    }

    private static final class Match {
        private static final Match NONE = new Match(null, false);
        private static final Match AMBIGUOUS = new Match(null, true);

        private final RecipeOption option;
        private final boolean ambiguous;

        private Match(RecipeOption option, boolean ambiguous) {
            this.option = option;
            this.ambiguous = ambiguous;
        }
    }
}
