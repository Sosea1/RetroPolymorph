package dev.sosea1.retropolymorph.client;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ClientRecipeCache {

    private SelectionContext context;
    private World world;
    private ItemStack[] snapshot = new ItemStack[0];
    private List<RecipeOption> choices = Collections.emptyList();

    boolean refresh(SelectionContext currentContext, World world) {
        if (this.context == currentContext
                && this.world == world
                && snapshotMatches(currentContext)) {
            return false;
        }

        this.context = currentContext;
        this.world = world;
        captureSnapshot(currentContext);
        this.choices = sanitizeOptions(currentContext.findOptions(world));
        return true;
    }

    List<RecipeOption> getChoices() {
        return this.choices;
    }

    void invalidate() {
        this.context = null;
        this.world = null;
    }

    static List<RecipeOption> sanitizeOptions(List<RecipeOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<RecipeOption> sanitized = null;
        for (int index = 0; index < options.size(); index++) {
            RecipeOption option = options.get(index);
            boolean valid = option != null
                    && RecipeKey.isWireSafe(option.getRecipeKey())
                    && !option.getOutput().isEmpty();
            if (valid) {
                if (sanitized != null) {
                    sanitized.add(option);
                }
                continue;
            }

            if (sanitized == null) {
                sanitized = new ArrayList<RecipeOption>(options.size() - 1);
                for (int previous = 0; previous < index; previous++) {
                    sanitized.add(options.get(previous));
                }
            }
        }

        return sanitized == null ? options : sanitized;
    }

    private boolean snapshotMatches(SelectionContext currentContext) {
        int size = currentContext.getInputCount();
        if (this.snapshot.length != size) {
            return false;
        }

        for (int slot = 0; slot < size; slot++) {
            if (!ItemStack.areItemStacksEqual(
                    this.snapshot[slot],
                    currentContext.getInputStack(slot))) {
                return false;
            }
        }
        return true;
    }

    private void captureSnapshot(SelectionContext currentContext) {
        int size = currentContext.getInputCount();
        if (this.snapshot.length != size) {
            this.snapshot = new ItemStack[size];
        }

        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = currentContext.getInputStack(slot);
            this.snapshot[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
    }
}
