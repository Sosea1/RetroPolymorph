package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeOption;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Default-selection policy for legacy FurnaceRecipes conflicts.
 *
 * <p>FurnaceRecipes in 1.12.2 does not expose registered recipe IDs, so this
 * policy deliberately uses output item IDs instead of RetroPolymorph's internal
 * synthetic smelt keys.</p>
 */
public final class SmeltingPreferencePolicy {

    private static final int MAX_ITEM_META = 32767;

    public static final class PolicySnapshot {
        private final List<String> preferredMods;
        private final List<String> preferredOutputs;
        private final boolean preferModdedOverVanilla;

        private PolicySnapshot(
                List<String> preferredMods,
                List<String> preferredOutputs,
                boolean preferModdedOverVanilla) {
            this.preferredMods = Collections.unmodifiableList(new ArrayList<String>(preferredMods));
            this.preferredOutputs = Collections.unmodifiableList(new ArrayList<String>(preferredOutputs));
            this.preferModdedOverVanilla = preferModdedOverVanilla;
        }

        public List<String> getPreferredMods() {
            return this.preferredMods;
        }

        public List<String> getPreferredOutputs() {
            return this.preferredOutputs;
        }

        public boolean isPreferModdedOverVanilla() {
            return this.preferModdedOverVanilla;
        }
    }

    private static volatile List<String> preferredMods = Collections.emptyList();
    private static volatile List<String> configuredPreferredOutputs = Collections.emptyList();
    private static volatile List<OutputSelector> preferredOutputs = Collections.emptyList();
    private static volatile boolean preferModdedOverVanilla = true;

    private SmeltingPreferencePolicy() {
    }

    public static PolicySnapshot getPolicySnapshot() {
        return new PolicySnapshot(
                preferredMods,
                configuredPreferredOutputs,
                preferModdedOverVanilla);
    }

    public static synchronized void configure(
            List<String> mods,
            List<String> outputs,
            boolean preferModded) {
        preferredMods = normalizePreferredMods(mods);

        ArrayList<String> cleanOutputStrings = new ArrayList<String>();
        ArrayList<OutputSelector> cleanOutputs = new ArrayList<OutputSelector>();
        if (outputs != null) {
            for (String raw : outputs) {
                OutputSelector selector = OutputSelector.parse(raw);
                if (selector == null || cleanOutputStrings.contains(selector.configValue)) {
                    continue;
                }
                cleanOutputStrings.add(selector.configValue);
                cleanOutputs.add(selector);
            }
        }

        configuredPreferredOutputs = cleanOutputStrings.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(cleanOutputStrings);
        preferredOutputs = cleanOutputs.isEmpty()
                ? Collections.<OutputSelector>emptyList()
                : Collections.unmodifiableList(cleanOutputs);
        preferModdedOverVanilla = preferModded;
    }

    /**
     * Sanitizes user-facing output selectors. Supported forms are
     * {@code modid:item} and {@code modid:item@meta}.
     */
    public static List<String> parsePreferredOutputs(String[] values) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        ArrayList<String> result = new ArrayList<String>();
        for (String raw : values) {
            OutputSelector selector = OutputSelector.parse(raw);
            if (selector != null && !result.contains(selector.configValue)) {
                result.add(selector.configValue);
            }
        }
        return result.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(result);
    }

    public static RecipePreferencePolicy.PreferenceDecision decide(List<RecipeOption> options) {
        if (options == null || options.isEmpty()) {
            return RecipePreferencePolicy.PreferenceDecision.none();
        }

        // 1. Exact output policy. Earlier configured selectors win.
        for (OutputSelector selector : preferredOutputs) {
            for (RecipeOption option : options) {
                if (option != null && selector.matches(option.getOutput())) {
                    return RecipePreferencePolicy.PreferenceDecision.exact(option.getRecipeKey());
                }
            }
        }

        // 2. Ordered output-mod priority.
        if (!preferredMods.isEmpty()) {
            RecipePreferencePolicy.PreferenceDecision modDecision = evaluatePreferredMods(options);
            if (modDecision != null) {
                return modDecision;
            }
        }

        // 3. Optional automatic modded-output preference.
        if (preferModdedOverVanilla) {
            String automatic = chooseFirstModdedOutput(options);
            if (automatic != null) {
                return RecipePreferencePolicy.PreferenceDecision.automaticModded(automatic);
            }
        }

        // 4. Native FurnaceRecipes order.
        return RecipePreferencePolicy.PreferenceDecision.none();
    }

    @Nullable
    private static RecipePreferencePolicy.PreferenceDecision evaluatePreferredMods(
            List<RecipeOption> options) {
        int wildcardIndex = preferredMods.indexOf("*");
        int bestBucket = Integer.MAX_VALUE;
        int[] buckets = new int[options.size()];

        for (int i = 0; i < options.size(); i++) {
            RecipeOption option = options.get(i);
            String namespace = option == null ? "" : outputNamespace(option.getOutput());
            if (namespace.isEmpty()) {
                buckets[i] = Integer.MAX_VALUE;
                continue;
            }

            int explicit = preferredMods.indexOf(namespace);
            if (explicit >= 0) {
                buckets[i] = explicit;
            } else if (wildcardIndex >= 0) {
                buckets[i] = wildcardIndex;
            } else {
                buckets[i] = preferredMods.size();
            }

            if (buckets[i] < bestBucket) {
                bestBucket = buckets[i];
            }
        }

        if (bestBucket == Integer.MAX_VALUE) {
            return null;
        }

        int countInBestBucket = 0;
        int firstInBestBucket = -1;
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] == bestBucket) {
                countInBestBucket++;
                if (firstInBestBucket < 0) {
                    firstInBestBucket = i;
                }
            }
        }

        if (countInBestBucket == options.size()) {
            return null;
        }

        return RecipePreferencePolicy.PreferenceDecision.modPriority(
                options.get(firstInBestBucket).getRecipeKey());
    }

    @Nullable
    private static String chooseFirstModdedOutput(List<RecipeOption> options) {
        boolean firstEligibleSeen = false;

        for (RecipeOption option : options) {
            if (option == null) {
                continue;
            }

            String namespace = outputNamespace(option.getOutput());
            if (namespace.isEmpty()) {
                continue;
            }

            boolean vanilla = "minecraft".equals(namespace);
            if (!firstEligibleSeen) {
                firstEligibleSeen = true;
                if (!vanilla) {
                    return null;
                }
                continue;
            }

            if (!vanilla) {
                return option.getRecipeKey();
            }
        }

        return null;
    }

    private static String outputNamespace(ItemStack output) {
        if (output == null || output.isEmpty()) {
            return "";
        }
        ResourceLocation id = output.getItem().getRegistryName();
        return id == null ? "" : id.getNamespace().toLowerCase(Locale.ROOT);
    }

    private static List<String> normalizePreferredMods(List<String> mods) {
        if (mods == null || mods.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<String> clean = new ArrayList<String>();
        boolean hasWildcard = false;
        for (String raw : mods) {
            if (raw == null) {
                continue;
            }
            String mod = raw.trim().toLowerCase(Locale.ROOT);
            if (mod.isEmpty()) {
                continue;
            }
            if ("*".equals(mod)) {
                if (!hasWildcard) {
                    clean.add("*");
                    hasWildcard = true;
                }
            } else if (!clean.contains(mod)) {
                clean.add(mod);
            }
        }

        if (!clean.isEmpty() && !hasWildcard) {
            clean.add("*");
        }
        return clean.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(clean);
    }

    private static final class OutputSelector {
        private final ResourceLocation itemId;
        @Nullable
        private final Integer metadata;
        private final String configValue;

        private OutputSelector(ResourceLocation itemId, @Nullable Integer metadata) {
            this.itemId = itemId;
            this.metadata = metadata;
            this.configValue = metadata == null
                    ? itemId.toString()
                    : itemId.toString() + "@" + metadata.intValue();
        }

        @Nullable
        static OutputSelector parse(@Nullable String raw) {
            if (raw == null) {
                return null;
            }

            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) {
                return null;
            }

            String itemPart = value;
            Integer metadata = null;
            int at = value.lastIndexOf('@');
            if (at > value.indexOf(':')) {
                if (at == value.length() - 1) {
                    return null;
                }
                try {
                    int parsed = Integer.parseInt(value.substring(at + 1));
                    if (parsed < 0 || parsed > MAX_ITEM_META) {
                        return null;
                    }
                    metadata = Integer.valueOf(parsed);
                } catch (NumberFormatException ignored) {
                    return null;
                }
                itemPart = value.substring(0, at);
            }

            ResourceLocation itemId;
            try {
                itemId = new ResourceLocation(itemPart);
            } catch (RuntimeException ignored) {
                return null;
            }

            if (!itemId.toString().equals(itemPart)) {
                return null;
            }
            return new OutputSelector(itemId, metadata);
        }

        boolean matches(ItemStack output) {
            if (output == null || output.isEmpty()) {
                return false;
            }
            ResourceLocation outputId = output.getItem().getRegistryName();
            return this.itemId.equals(outputId)
                    && (this.metadata == null
                    || this.metadata.intValue() == output.getMetadata());
        }
    }
}
