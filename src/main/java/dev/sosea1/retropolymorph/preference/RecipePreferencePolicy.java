package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import net.minecraft.util.ResourceLocation;

import dev.sosea1.retropolymorph.api.SelectionReason;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Server-side default-recipe policy for modpacks and integration addons.
 *
 * Evaluation hierarchy:
 * 1. Valid current owner/session selection (handled in SelectionService)
 * 2. Stored explicit player preference (handled in SelectionService)
 * 3. Exact recipe policy (API registrations and configured exact recipes) -> EXACT_RECIPE_POLICY
 * 4. Ordered preferredMods policy (UniDict-style with wildcard '*') -> MOD_PRIORITY
 * 5. Automatic "prefer modded over vanilla natural default" rule -> AUTOMATIC_MODDED
 * 6. Native Forge/machine default order -> NATIVE_DEFAULT
 */
public final class RecipePreferencePolicy {

    /**
     * Structured result explaining the recipe choice and the reason it was chosen.
     */
    public static final class PreferenceDecision {
        @Nullable
        private final String recipeKey;
        private final SelectionReason reason;

        public PreferenceDecision(@Nullable String recipeKey, SelectionReason reason) {
            this.recipeKey = recipeKey;
            this.reason = reason == null ? SelectionReason.NATIVE_DEFAULT : reason;
        }

        @Nullable
        public String getRecipeKey() {
            return this.recipeKey;
        }

        public SelectionReason getReason() {
            return this.reason;
        }

        public static PreferenceDecision none() {
            return new PreferenceDecision(null, SelectionReason.NATIVE_DEFAULT);
        }

        public static PreferenceDecision exact(String key) {
            return new PreferenceDecision(key, SelectionReason.EXACT_RECIPE_POLICY);
        }

        public static PreferenceDecision modPriority(String key) {
            return new PreferenceDecision(key, SelectionReason.MOD_PRIORITY);
        }

        public static PreferenceDecision automaticModded(String key) {
            return new PreferenceDecision(key, SelectionReason.AUTOMATIC_MODDED);
        }

        @Override
        public String toString() {
            return "PreferenceDecision{key='" + this.recipeKey + "', reason=" + this.reason + '}';
        }
    }

    /**
     * Immutable snapshot of the effective preference policy configuration.
     */
    public static final class PolicySnapshot {
        private final List<String> preferredMods;
        private final List<String> configuredExactRecipes;
        private final Map<String, Integer> addonRecipeOverrides;
        private final boolean preferModdedOverVanilla;

        public PolicySnapshot(
                List<String> preferredMods,
                List<String> configuredExactRecipes,
                Map<String, Integer> addonRecipeOverrides,
                boolean preferModdedOverVanilla) {
            this.preferredMods = preferredMods == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(preferredMods));
            this.configuredExactRecipes = configuredExactRecipes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(configuredExactRecipes));
            this.addonRecipeOverrides = addonRecipeOverrides == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(addonRecipeOverrides));
            this.preferModdedOverVanilla = preferModdedOverVanilla;
        }

        public List<String> getPreferredMods() {
            return this.preferredMods;
        }

        public List<String> getConfiguredExactRecipes() {
            return this.configuredExactRecipes;
        }

        public Map<String, Integer> getAddonRecipeOverrides() {
            return this.addonRecipeOverrides;
        }

        public boolean isPreferModdedOverVanilla() {
            return this.preferModdedOverVanilla;
        }

        @Override
        public String toString() {
            return "PolicySnapshot{preferredMods=" + this.preferredMods
                    + ", configuredExactRecipes=" + this.configuredExactRecipes
                    + ", addonRecipeOverrides=" + this.addonRecipeOverrides
                    + ", preferModdedOverVanilla=" + this.preferModdedOverVanilla + "}";
        }
    }

    private static volatile List<String> preferredMods = Collections.emptyList();
    private static volatile List<String> configuredExactRecipes = Collections.emptyList();
    private static volatile Map<String, Integer> registeredPriorities = Collections.emptyMap();
    private static volatile boolean preferModdedOverVanilla = true;

    private RecipePreferencePolicy() {
    }

    public static PolicySnapshot getPolicySnapshot() {
        return new PolicySnapshot(
                preferredMods,
                configuredExactRecipes,
                registeredPriorities,
                preferModdedOverVanilla);
    }

    public static synchronized void configure(List<String> mods, List<String> exactRecipes) {
        configure(mods, exactRecipes, preferModdedOverVanilla);
    }

    /**
     * Configures the ordered preferred mods, exact recipes and automatic fallback from config.
     *
     * @param mods ordered mod namespaces (with '*' representing unlisted mods)
     * @param exactRecipes ordered list of recipe keys (earlier entries win)
     * @param preferModded whether a modded option should replace a vanilla native default
     */
    public static synchronized void configure(
            List<String> mods,
            List<String> exactRecipes,
            boolean preferModded) {
        preferModdedOverVanilla = preferModded;
        if (mods == null || mods.isEmpty()) {
            preferredMods = Collections.emptyList();
        } else {
            ArrayList<String> cleanMods = new ArrayList<String>();
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
                        cleanMods.add("*");
                        hasWildcard = true;
                    }
                } else if (!cleanMods.contains(mod)) {
                    cleanMods.add(mod);
                }
            }
            if (!cleanMods.isEmpty() && !hasWildcard) {
                // Wildcard was omitted; append implicit '*' at end of list
                cleanMods.add("*");
            }
            preferredMods = Collections.unmodifiableList(cleanMods);
        }

        if (exactRecipes == null || exactRecipes.isEmpty()) {
            configuredExactRecipes = Collections.emptyList();
        } else {
            ArrayList<String> cleanExact = new ArrayList<String>();
            for (String raw : exactRecipes) {
                if (raw == null) {
                    continue;
                }
                String key = raw.trim();
                if (RecipeKey.isWireSafe(key) && !cleanExact.contains(key)) {
                    cleanExact.add(key);
                }
            }
            configuredExactRecipes = Collections.unmodifiableList(cleanExact);
        }
    }

    /**
     * API/addon priority. A value of zero removes that API override.
     */
    public static synchronized void setPriority(ResourceLocation recipeId, int priority) {
        if (recipeId == null) {
            throw new NullPointerException("recipeId");
        }
        setPriority(recipeId.toString(), priority);
    }

    /**
     * API/addon priority. A value of zero removes that API override.
     */
    public static synchronized void setPriority(String recipeKey, int priority) {
        if (!RecipeKey.isWireSafe(recipeKey)) {
            throw new IllegalArgumentException("Unsafe recipe key: " + recipeKey);
        }
        if (priority < 0) {
            throw new IllegalArgumentException("Priority must be non-negative: " + priority);
        }
        LinkedHashMap<String, Integer> copy =
                new LinkedHashMap<String, Integer>(registeredPriorities);
        if (priority == 0) {
            copy.remove(recipeKey);
        } else {
            copy.put(recipeKey, priority);
        }
        registeredPriorities = immutable(copy);
    }

    public static List<String> getPreferredMods() {
        return preferredMods;
    }

    public static List<String> getConfiguredExactRecipes() {
        return configuredExactRecipes;
    }

    /**
     * Evaluates policy against the given conflicting options and returns a structured decision.
     */
    public static PreferenceDecision decide(List<RecipeOption> options) {
        if (options == null || options.isEmpty()) {
            return PreferenceDecision.none();
        }

        // 1. Exact recipe policy: registered API overrides first (highest positive priority wins)
        if (!registeredPriorities.isEmpty()) {
            String bestKey = null;
            int bestPriority = 0;
            for (RecipeOption option : options) {
                if (option == null) {
                    continue;
                }
                Integer p = registeredPriorities.get(option.getRecipeKey());
                if (p != null && p.intValue() > bestPriority) {
                    bestPriority = p.intValue();
                    bestKey = option.getRecipeKey();
                }
            }
            if (bestKey != null) {
                return PreferenceDecision.exact(bestKey);
            }
        }

        // 2. Exact recipe policy: configured exact recipes (earlier in list wins)
        if (!configuredExactRecipes.isEmpty()) {
            for (String candidate : configuredExactRecipes) {
                for (RecipeOption option : options) {
                    if (option != null && candidate.equals(option.getRecipeKey())) {
                        return PreferenceDecision.exact(option.getRecipeKey());
                    }
                }
            }
        }

        // 3. Ordered preferredMods policy
        if (!preferredMods.isEmpty()) {
            PreferenceDecision modDecision = evaluatePreferredMods(options);
            if (modDecision != null) {
                return modDecision;
            }
        }

        // 4. Optional "prefer modded over vanilla natural default" rule
        if (preferModdedOverVanilla) {
            String modernDefault = chooseFirstModdedRecipe(options);
            if (modernDefault != null) {
                return PreferenceDecision.automaticModded(modernDefault);
            }
        }

        // 5. Native Forge / machine default
        return PreferenceDecision.none();
    }

    @Nullable
    public static String choose(List<RecipeOption> options) {
        return decide(options).getRecipeKey();
    }

    @Nullable
    private static PreferenceDecision evaluatePreferredMods(List<RecipeOption> options) {
        int bestBucket = Integer.MAX_VALUE;
        int wildcardIndex = preferredMods.indexOf("*");

        int[] buckets = new int[options.size()];
        for (int i = 0; i < options.size(); i++) {
            RecipeOption option = options.get(i);
            if (option == null) {
                buckets[i] = Integer.MAX_VALUE;
                continue;
            }
            String namespace = policyNamespace(option);
            if (namespace.isEmpty()) {
                buckets[i] = Integer.MAX_VALUE;
                continue;
            }
            int idx = preferredMods.indexOf(namespace);
            if (idx >= 0) {
                buckets[i] = idx;
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
        int firstInBestBucketIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            if (buckets[i] == bestBucket) {
                countInBestBucket++;
                if (firstInBestBucketIndex < 0) {
                    firstInBestBucketIndex = i;
                }
            }
        }

        // If all options landed in the exact same priority bucket, mod priority does not differentiate.
        if (countInBestBucket == options.size()) {
            return null;
        }

        // Mod priority successfully eliminated worse buckets. Take first option in the winning bucket.
        RecipeOption winner = options.get(firstInBestBucketIndex);
        return PreferenceDecision.modPriority(winner.getRecipeKey());
    }

    @Nullable
    private static String chooseFirstModdedRecipe(List<RecipeOption> options) {
        boolean firstEligibleRecipeSeen = false;
        boolean naturalDefaultIsVanilla = false;

        for (RecipeOption option : options) {
            if (option == null) {
                continue;
            }

            String namespace = automaticPreferenceNamespace(option);
            if (namespace.isEmpty()) {
                continue;
            }

            boolean vanilla = "minecraft".equals(namespace);
            if (!firstEligibleRecipeSeen) {
                firstEligibleRecipeSeen = true;
                naturalDefaultIsVanilla = vanilla;
                if (!vanilla) {
                    return null;
                }
                continue;
            }

            if (naturalDefaultIsVanilla && !vanilla) {
                return option.getRecipeKey();
            }
        }
        return null;
    }

    private static String automaticPreferenceNamespace(RecipeOption option) {
        String explicitNamespace = option.getPolicyNamespace();
        if (explicitNamespace != null && !explicitNamespace.isEmpty()) {
            return explicitNamespace;
        }

        ResourceLocation id;
        try {
            id = RecipeKey.parseForgeId(option.getRecipeKey());
            if (id == null) {
                return "";
            }
            try {
                if (ForgeRegistries.RECIPES != null && ForgeRegistries.RECIPES.getValue(id) == null) {
                    return "";
                }
            } catch (LinkageError | RuntimeException ignored) {
                // Running in a unit-test environment without full Forge registries.
            }
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
        return id.getNamespace();
    }

    private static String policyNamespace(RecipeOption option) {
        String explicitNamespace = option.getPolicyNamespace();
        return explicitNamespace == null || explicitNamespace.isEmpty()
                ? extractNamespace(option.getRecipeKey())
                : explicitNamespace;
    }

    public static String extractNamespace(String recipeKey) {
        if (recipeKey == null) {
            return "";
        }
        int colon = recipeKey.indexOf(':');
        if (colon > 0) {
            return recipeKey.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static Map<String, Integer> immutable(LinkedHashMap<String, Integer> values) {
        return values.isEmpty()
                ? Collections.<String, Integer>emptyMap()
                : Collections.unmodifiableMap(values);
    }
}
