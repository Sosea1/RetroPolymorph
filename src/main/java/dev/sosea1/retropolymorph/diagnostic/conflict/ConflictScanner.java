package dev.sosea1.retropolymorph.diagnostic.conflict;

import dev.sosea1.retropolymorph.furnace.FurnaceConflictExtension;
import dev.sosea1.retropolymorph.furnace.FurnaceConflictRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Explicit cold-path conflict diagnostics.
 *
 * Crafting uses normalized ingredient signatures and therefore reports
 * candidates, not mathematical proof for arbitrary custom IRecipe logic.
 */
public final class ConflictScanner {

    private static final int WILDCARD_META = 32767;

    private ConflictScanner() {
    }

    public static ConflictReport scan() {
        CraftingScan crafting = scanCrafting();
        FurnaceScan furnace = scanFurnace();
        return new ConflictReport(
                crafting.scanned,
                crafting.groups,
                furnace.scanned,
                furnace.pairs);
    }

    private static CraftingScan scanCrafting() {
        LinkedHashMap<String, CraftingBucket> buckets =
                new LinkedHashMap<String, CraftingBucket>();
        int scanned = 0;

        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            scanned++;
            String signature = craftingSignature(recipe);
            if (signature == null) {
                continue;
            }

            ItemStack output = recipe.getRecipeOutput();
            String outputSignature = stackSignature(output);
            if (outputSignature == null) {
                continue;
            }

            CraftingBucket bucket = buckets.get(signature);
            if (bucket == null) {
                bucket = new CraftingBucket(compactCraftingDescription(recipe));
                buckets.put(signature, bucket);
            }
            bucket.add(recipeName(recipe), outputSignature);
        }

        ArrayList<CraftingConflictGroup> groups = new ArrayList<CraftingConflictGroup>();
        for (CraftingBucket bucket : buckets.values()) {
            if (bucket.outputSignatures.size() <= 1 || bucket.recipes.size() <= 1) {
                continue;
            }
            Collections.sort(bucket.recipes);
            groups.add(new CraftingConflictGroup(bucket.description, bucket.recipes));
        }
        Collections.sort(groups, new Comparator<CraftingConflictGroup>() {
            @Override
            public int compare(CraftingConflictGroup first, CraftingConflictGroup second) {
                int byInput = first.getInputDescription().compareTo(second.getInputDescription());
                if (byInput != 0) {
                    return byInput;
                }
                return first.getRecipes().get(0).compareTo(second.getRecipes().get(0));
            }
        });
        return new CraftingScan(scanned, groups);
    }

    private static FurnaceScan scanFurnace() {
        FurnaceRecipes recipes = FurnaceRecipes.instance();
        Map<Item, FurnaceItemBucket> buckets = new HashMap<Item, FurnaceItemBucket>();
        int scanned = 0;

        for (Map.Entry<ItemStack, ItemStack> entry : recipes.getSmeltingList().entrySet()) {
            scanned++;
            ItemStack input = entry.getKey();
            ItemStack output = entry.getValue();
            if (input.isEmpty() || output.isEmpty()) {
                continue;
            }
            FurnaceItemBucket bucket = buckets.get(input.getItem());
            if (bucket == null) {
                bucket = new FurnaceItemBucket();
                buckets.put(input.getItem(), bucket);
            }
            bucket.live.add(new FurnaceEntry(input, output, "live"));
        }

        if (recipes instanceof FurnaceConflictExtension) {
            for (FurnaceConflictRecipe conflict
                    : ((FurnaceConflictExtension) recipes).retropolymorph$getRejectedConflicts()) {
                scanned++;
                ItemStack input = conflict.getInput();
                ItemStack output = conflict.getOutput();
                if (input.isEmpty() || output.isEmpty()) {
                    continue;
                }
                FurnaceItemBucket bucket = buckets.get(input.getItem());
                if (bucket == null) {
                    bucket = new FurnaceItemBucket();
                    buckets.put(input.getItem(), bucket);
                }
                bucket.rejected.add(new FurnaceEntry(input, output, "rejected-registration"));
            }
        }

        ArrayList<FurnaceConflictPair> pairs = new ArrayList<FurnaceConflictPair>();
        HashSet<String> dedupe = new HashSet<String>();
        for (FurnaceItemBucket bucket : buckets.values()) {
            addLiveOverlaps(bucket.live, pairs, dedupe);
            addRejectedOverlaps(bucket.live, bucket.rejected, pairs, dedupe);
        }
        Collections.sort(pairs, new Comparator<FurnaceConflictPair>() {
            @Override
            public int compare(FurnaceConflictPair first, FurnaceConflictPair second) {
                int byInput = first.getInputDescription().compareTo(second.getInputDescription());
                if (byInput != 0) {
                    return byInput;
                }
                int byFirst = first.getFirst().compareTo(second.getFirst());
                return byFirst != 0 ? byFirst : first.getSecond().compareTo(second.getSecond());
            }
        });
        return new FurnaceScan(scanned, pairs);
    }

    private static void addLiveOverlaps(
            List<FurnaceEntry> live,
            List<FurnaceConflictPair> output,
            Set<String> dedupe) {
        FurnaceEntry wildcard = null;
        HashMap<Integer, FurnaceEntry> exact = new HashMap<Integer, FurnaceEntry>();
        for (FurnaceEntry entry : live) {
            if (entry.input.getMetadata() == WILDCARD_META) {
                wildcard = entry;
            } else {
                exact.put(Integer.valueOf(entry.input.getMetadata()), entry);
            }
        }
        if (wildcard == null) {
            return;
        }
        for (FurnaceEntry entry : exact.values()) {
            if (!sameOutput(wildcard.output, entry.output)) {
                addFurnacePair(entry.input, wildcard, entry, output, dedupe);
            }
        }
    }

    private static void addRejectedOverlaps(
            List<FurnaceEntry> live,
            List<FurnaceEntry> rejected,
            List<FurnaceConflictPair> output,
            Set<String> dedupe) {
        for (FurnaceEntry conflict : rejected) {
            for (FurnaceEntry accepted : live) {
                if (!inputsOverlap(conflict.input, accepted.input)
                        || sameOutput(conflict.output, accepted.output)) {
                    continue;
                }
                addFurnacePair(conflict.input, accepted, conflict, output, dedupe);
            }
        }
    }

    private static void addFurnacePair(
            ItemStack input,
            FurnaceEntry first,
            FurnaceEntry second,
            List<FurnaceConflictPair> output,
            Set<String> dedupe) {
        String firstDescription = first.describe();
        String secondDescription = second.describe();
        if (firstDescription.compareTo(secondDescription) > 0) {
            String swap = firstDescription;
            firstDescription = secondDescription;
            secondDescription = swap;
        }

        String inputDescription = stackInputDescription(input);
        String key = inputDescription + '\u0000' + firstDescription + '\u0000' + secondDescription;
        if (dedupe.add(key)) {
            output.add(new FurnaceConflictPair(
                    inputDescription, firstDescription, secondDescription));
        }
    }

    private static String craftingSignature(IRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            return null;
        }

        ArrayList<String> parts = new ArrayList<String>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            parts.add(ingredientSignature(ingredient));
        }

        if (recipe instanceof IShapedRecipe) {
            IShapedRecipe shaped = (IShapedRecipe) recipe;
            return "S:" + shaped.getRecipeWidth() + 'x' + shaped.getRecipeHeight()
                    + ':' + join(parts);
        }

        Collections.sort(parts);
        return "U:" + parts.size() + ':' + join(parts);
    }

    private static String ingredientSignature(Ingredient ingredient) {
        if (ingredient == null) {
            return "[]";
        }
        ItemStack[] matching = ingredient.getMatchingStacks();
        if (matching == null || matching.length == 0) {
            return "[]";
        }

        ArrayList<String> variants = new ArrayList<String>(matching.length);
        for (ItemStack stack : matching) {
            String signature = stackIngredientSignature(stack);
            if (signature != null) {
                variants.add(signature);
            }
        }
        Collections.sort(variants);
        return '[' + join(variants) + ']';
    }

    private static String compactCraftingDescription(IRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int slots = ingredients == null ? 0 : ingredients.size();
        if (recipe instanceof IShapedRecipe) {
            IShapedRecipe shaped = (IShapedRecipe) recipe;
            return "shaped " + shaped.getRecipeWidth() + 'x' + shaped.getRecipeHeight();
        }
        return "shapeless slots=" + slots;
    }

    private static String recipeName(IRecipe recipe) {
        ResourceLocation id = recipe.getRegistryName();
        String output = stackDescription(recipe.getRecipeOutput());
        return (id == null ? "<unregistered:" + recipe.getClass().getName() + '>' : id.toString())
                + " -> " + output;
    }

    private static String stackIngredientSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null) {
            return null;
        }
        String nbt = stack.hasTagCompound()
                ? "#nbt:" + Integer.toHexString(stack.getTagCompound().hashCode())
                : "";
        return id + "@" + stack.getMetadata() + nbt;
    }

    private static String stackSignature(ItemStack stack) {
        String base = stackIngredientSignature(stack);
        return base == null ? null : base + 'x' + stack.getCount();
    }

    private static String stackDescription(ItemStack stack) {
        String signature = stackSignature(stack);
        return signature == null ? "<empty>" : signature;
    }

    private static String stackInputDescription(ItemStack stack) {
        String signature = stackIngredientSignature(stack);
        return signature == null ? "<empty>" : signature;
    }

    private static boolean inputsOverlap(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty() || first.getItem() != second.getItem()) {
            return false;
        }
        int firstMeta = first.getMetadata();
        int secondMeta = second.getMetadata();
        return firstMeta == secondMeta || firstMeta == WILDCARD_META || secondMeta == WILDCARD_META;
    }

    private static boolean sameOutput(ItemStack first, ItemStack second) {
        return ItemStack.areItemStacksEqual(first, second);
    }

    private static String join(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append('|');
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private static final class CraftingBucket {
        private final String description;
        private final List<String> recipes = new ArrayList<String>();
        private final Set<String> outputSignatures = new HashSet<String>();

        private CraftingBucket(String description) {
            this.description = description;
        }

        private void add(String recipe, String outputSignature) {
            this.recipes.add(recipe);
            this.outputSignatures.add(outputSignature);
        }
    }

    private static final class FurnaceEntry {
        private final ItemStack input;
        private final ItemStack output;
        private final String source;

        private FurnaceEntry(ItemStack input, ItemStack output, String source) {
            this.input = input;
            this.output = output;
            this.source = source;
        }

        private String describe() {
            return this.source + ": " + stackDescription(this.output);
        }
    }

    private static final class FurnaceItemBucket {
        private final List<FurnaceEntry> live = new ArrayList<FurnaceEntry>();
        private final List<FurnaceEntry> rejected = new ArrayList<FurnaceEntry>();
    }

    private static final class CraftingScan {
        private final int scanned;
        private final List<CraftingConflictGroup> groups;

        private CraftingScan(int scanned, List<CraftingConflictGroup> groups) {
            this.scanned = scanned;
            this.groups = groups;
        }
    }

    private static final class FurnaceScan {
        private final int scanned;
        private final List<FurnaceConflictPair> pairs;

        private FurnaceScan(int scanned, List<FurnaceConflictPair> pairs) {
            this.scanned = scanned;
            this.pairs = pairs;
        }
    }
}
