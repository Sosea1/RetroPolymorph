package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.core.SelectionContextDetector;
import dev.sosea1.retropolymorph.network.NetworkHandler;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Short-lived intent captured from a successful JEI recipe transfer. */
public final class JeiTransferIntent {

    private static final long MAX_AGE_NANOS = 10_000_000_000L;
    private static final int MAX_OUTPUT_VARIANTS = 32;

    private static Container targetContainer;
    private static ItemStack[] beforeInputs = new ItemStack[0];
    private static List<ItemStack> intendedOutputs = Collections.emptyList();
    private static String intendedRecipeKey;
    private static long createdAtNanos;

    private JeiTransferIntent() {
    }

    public static void capture(Container container, String recipeKey, List<ItemStack> outputs) {
        if (container == null || (recipeKey == null && (outputs == null || outputs.isEmpty()))) {
            clear();
            return;
        }

        ArrayList<ItemStack> unique = new ArrayList<ItemStack>();
        if (outputs != null) {
            for (ItemStack output : outputs) {
                if (output == null || output.isEmpty() || containsStack(unique, output)) {
                    continue;
                }
                unique.add(output.copy());
                if (unique.size() >= MAX_OUTPUT_VARIANTS) {
                    break;
                }
            }
        }
        if (recipeKey == null && unique.isEmpty()) {
            clear();
            return;
        }

        targetContainer = container;
        intendedRecipeKey = recipeKey;
        intendedOutputs = Collections.unmodifiableList(unique);
        createdAtNanos = System.nanoTime();
        SelectionContext context = SelectionContextDetector.detect(container);
        beforeInputs = context == null ? new ItemStack[0] : snapshot(context);
    }

    static boolean tryApply(
            SelectionContext context,
            List<RecipeOption> choices,
            int sessionToken,
            int inputRevision) {
        if (targetContainer == null) {
            return false;
        }
        if (isExpired() || context.getContainer() != targetContainer) {
            clear();
            return false;
        }

        Match match = findMatch(choices, intendedRecipeKey, intendedOutputs);
        if (match.ambiguous) {
            clear();
            return false;
        }
        if (match.option == null) {
            // A first reply can still describe the pre-transfer input. Once the
            // client matrix has actually changed, a server snapshot without the
            // intended output is authoritative and the intent can be dropped.
            if (beforeInputs.length > 0 && !snapshotMatches(context, beforeInputs)) {
                clear();
            }
            return false;
        }

        clear();
        NetworkHandler.select(
                context.getContainer().windowId,
                sessionToken,
                inputRevision,
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
        intendedRecipeKey = null;
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

    private static Match findMatch(
            List<RecipeOption> choices,
            String recipeKey,
            List<ItemStack> outputs) {
        if (recipeKey != null) {
            for (RecipeOption choice : choices) {
                if (recipeKey.equals(choice.getRecipeKey())) {
                    return new Match(choice, false);
                }
            }
            // An exact JEI identity is stronger than output equality. Falling
            // back to output here could select a different conflicting recipe.
            return Match.NONE;
        }

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
