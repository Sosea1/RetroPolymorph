package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.api.SelectionReason;
import dev.sosea1.retropolymorph.preference.ConflictFingerprint;
import dev.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import dev.sosea1.retropolymorph.preference.RecipePreferencePolicy;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
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

        if (command.isClear()) {
            if (persistence.supportsPlayerPreferences() && fingerprint != null && playerEntityData != null) {
                PlayerRecipePreferences.forget(playerEntityData, fingerprint);
            }
            SelectionContextGuard.clear(context);
            reason = applyPolicyDefault(world, context, options);
        } else if (command.isSelect()) {
            String recipeKey = command.getRecipeKey();
            accepted = recipeKey != null
                    && containsOption(options, recipeKey)
                    && SelectionContextGuard.select(context, recipeKey, world);
            if (accepted) {
                if (persistence.supportsPlayerPreferences() && fingerprint != null && playerEntityData != null) {
                    PlayerRecipePreferences.remember(playerEntityData, fingerprint, recipeKey);
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
                    && (current == null || persistence.playerPreferenceOverridesCurrent())) {
                if (preferred.equals(current)) {
                    return SelectionReason.PLAYER_PREFERENCE;
                }
                if (containsOption(options, preferred)
                        && SelectionContextGuard.select(context, preferred, world)) {
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

    private static boolean sameRecipeKey(@Nullable String first, @Nullable String second) {
        return first == null ? second == null : first.equals(second);
    }
}
