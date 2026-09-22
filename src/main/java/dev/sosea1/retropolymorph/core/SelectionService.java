package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.preference.ConflictFingerprint;
import dev.sosea1.retropolymorph.preference.InputFingerprint;
import dev.sosea1.retropolymorph.preference.InputPreferenceKeys;
import dev.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Generic server-side recipe selection service.
 * Handles query, select, clear, player preferences, modpack policy, and automatic fallback
 * independently of network packets or GUI session tracking.
 */
public final class SelectionService {

    private SelectionService() {
    }

    public static SelectionServiceResult handle(
            @Nullable EntityPlayerMP player,
            SelectionContext context,
            SelectionCommand command) {
        return handle(
                player != null ? player.world : null,
                player != null ? player.getEntityData() : null,
                context,
                command);
    }

    public static SelectionServiceResult handle(
            @Nullable World world,
            @Nullable NBTTagCompound playerEntityData,
            SelectionContext context,
            SelectionCommand command) {
        if (context == null || command == null) {
            return new SelectionServiceResult(
                    false, Collections.<RecipeOption>emptyList(), null, SelectionReason.NATIVE_DEFAULT, false);
        }

        String inputFingerprint = InputFingerprint.create(context);
        List<RecipeOption> options = SelectionContextGuard.findOptions(context, world);
        if (options == null) {
            SelectionContextGuard.clear(context);
            return new SelectionServiceResult(
                    false, Collections.<RecipeOption>emptyList(), null, SelectionReason.NATIVE_DEFAULT, false);
        }

        String fingerprint = ConflictFingerprint.create(options);
        String selectedBefore = SelectionContextGuard.selected(context);
        SelectionPersistencePolicy persistence = context.getPersistencePolicy();
        boolean accepted = true;
        SelectionReason reason = SelectionReason.NATIVE_DEFAULT;
        boolean staleSelectionCleared = command.isQuery()
                && selectedBefore != null
                && !containsOption(options, selectedBefore);

        if (staleSelectionCleared) {
            // Preseeding runs before CraftingManager would normally notice that
            // the matrix changed. Drop the old matrix choice now so it cannot
            // suppress the saved preference for this new conflict.
            SelectionContextGuard.clear(context);
            CraftingPreferenceSeeder.clearProvisional(context);
        }

        if (command.isClear()) {
            if (persistence.supportsPlayerPreferences() && playerEntityData != null) {
                String rememberedRecipe = fingerprint == null
                        ? selectedBefore
                        : PlayerRecipePreferences.lookup(playerEntityData, fingerprint);
                if (fingerprint != null) {
                    PlayerRecipePreferences.forget(playerEntityData, fingerprint);
                }
                forgetInputAliases(playerEntityData, context, rememberedRecipe, world);
                if (inputFingerprint != null) {
                    PlayerRecipePreferences.forgetInput(playerEntityData, inputFingerprint);
                }
            }
            SelectionContextGuard.clear(context);
            CraftingPreferenceSeeder.clearProvisional(context);
            reason = applyPolicyDefault(world, context, options);
        } else if (command.isSelect()) {
            String recipeKey = command.getRecipeKey();
            accepted = recipeKey != null
                    && containsOption(options, recipeKey)
                    && SelectionContextGuard.select(context, recipeKey, world);
            if (accepted) {
                CraftingPreferenceSeeder.clearProvisional(context);
                if (persistence.supportsPlayerPreferences() && playerEntityData != null) {
                    if (fingerprint != null) {
                        PlayerRecipePreferences.remember(playerEntityData, fingerprint, recipeKey);
                    }
                    rememberInputAliases(playerEntityData, context, recipeKey, world);
                }
                reason = SelectionReason.PLAYER_SELECTION;
            } else {
                reason = SelectionReason.NATIVE_DEFAULT;
            }
        } else if (command.isQuery()) {
            reason = handleQuery(world, playerEntityData, context, options, fingerprint, persistence);
        } else {
            accepted = false;
        }

        String selectedAfter = SelectionContextGuard.selected(context);
        if (selectedAfter != null
                && (!RecipeKey.isWireSafe(selectedAfter) || !containsOption(options, selectedAfter))) {
            // Drop authoritative selection that no longer belongs to the live options.
            SelectionContextGuard.clear(context);
            selectedAfter = null;
            accepted = false;
            reason = SelectionReason.NATIVE_DEFAULT;
        }

        if (command.isQuery()
                && reason == SelectionReason.PLAYER_PREFERENCE
                && selectedAfter != null
                && inputFingerprint != null
                && persistence.supportsPlayerPreferences()
                && playerEntityData != null) {
            // Populate/refresh the cheap recipe-aware input aliases after
            // the canonical conflict preference has been authoritatively resolved.
            rememberInputAliases(playerEntityData, context, selectedAfter, world);
        }

        if (staleSelectionCleared && selectedAfter == null) {
            // Preserve the query contract: merely discarding a stale state is
            // not an accepted selection. A valid preference or policy choice
            // selected above remains accepted.
            accepted = false;
        }

        boolean selectionChanged = !sameRecipeKey(selectedBefore, selectedAfter);
        return new SelectionServiceResult(accepted, options, selectedAfter, reason, selectionChanged);
    }

    private static SelectionReason handleQuery(
            @Nullable World world,
            @Nullable NBTTagCompound playerEntityData,
            SelectionContext context,
            List<RecipeOption> options,
            @Nullable String fingerprint,
            SelectionPersistencePolicy persistence) {
        String current = SelectionContextGuard.selected(context);

        if (fingerprint != null && persistence.supportsPlayerPreferences() && playerEntityData != null) {
            String preferred = PlayerRecipePreferences.lookup(playerEntityData, fingerprint);
            if (preferred != null
                    && (current == null
                    || persistence.playerPreferenceOverridesCurrent()
                    || CraftingPreferenceSeeder.isProvisional(context, current))) {
                if (preferred.equals(current)) {
                    CraftingPreferenceSeeder.clearProvisional(context);
                    return SelectionReason.PLAYER_PREFERENCE;
                }
                if (containsOption(options, preferred)
                        && SelectionContextGuard.select(context, preferred, world)) {
                    CraftingPreferenceSeeder.clearProvisional(context);
                    return SelectionReason.PLAYER_PREFERENCE;
                }
                PlayerRecipePreferences.forget(playerEntityData, fingerprint);
            }
        }

        if (current != null) {
            return SelectionReason.CURRENT_CONTEXT;
        }

        return applyPolicyDefault(world, context, options);
    }

    private static SelectionReason applyPolicyDefault(
            @Nullable World world,
            SelectionContext context,
            List<RecipeOption> options) {
        RecipePreferencePolicy.PreferenceDecision decision = RecipePreferencePolicy.decide(options);
        String policyChoice = decision.getRecipeKey();
        if (policyChoice != null
                && containsOption(options, policyChoice)
                && SelectionContextGuard.select(context, policyChoice, world)) {
            return decision.getReason();
        }

        return SelectionReason.NATIVE_DEFAULT;
    }

    @Nullable
    public static String chooseFirstModdedForgeRecipe(List<RecipeOption> options) {
        return RecipePreferencePolicy.chooseFirstModdedForgeRecipe(options);
    }

    public static boolean containsOption(List<RecipeOption> options, String recipeKey) {
        if (!RecipeKey.isWireSafe(recipeKey)) {
            return false;
        }
        for (RecipeOption option : options) {
            if (option != null && recipeKey.equals(option.getRecipeKey())) {
                return true;
            }
        }
        return false;
    }

    private static void rememberInputAliases(
            NBTTagCompound playerEntityData,
            SelectionContext context,
            String recipeKey,
            @Nullable World world) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        IRecipe recipe = recipeId == null ? null : ForgeRegistries.RECIPES.getValue(recipeId);
        for (String key : InputPreferenceKeys.storageKeys(context, recipe, world)) {
            PlayerRecipePreferences.rememberInput(playerEntityData, key, recipeKey);
        }
    }

    private static void forgetInputAliases(
            NBTTagCompound playerEntityData,
            SelectionContext context,
            @Nullable String recipeKey,
            @Nullable World world) {
        ResourceLocation recipeId = RecipeKey.parseForgeId(recipeKey);
        IRecipe recipe = recipeId == null ? null : ForgeRegistries.RECIPES.getValue(recipeId);
        for (String key : InputPreferenceKeys.storageKeys(context, recipe, world)) {
            PlayerRecipePreferences.forgetInput(playerEntityData, key);
        }
    }

    private static boolean sameRecipeKey(@Nullable String first, @Nullable String second) {
        return first == null ? second == null : first.equals(second);
    }
}
