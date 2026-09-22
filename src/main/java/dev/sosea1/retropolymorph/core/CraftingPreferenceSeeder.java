package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.AdapterDetectionResult;
import dev.sosea1.retropolymorph.api.SelectionContext;
import dev.sosea1.retropolymorph.api.SelectionPersistencePolicy;
import dev.sosea1.retropolymorph.mixin.SlotCraftingAccessor;
import dev.sosea1.retropolymorph.preference.InputFingerprint;
import dev.sosea1.retropolymorph.preference.InputPreferenceKeys;
import dev.sosea1.retropolymorph.preference.PlayerRecipePreferences;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Restores a previously selected recipe without enumerating the whole conflict.
 *
 * <p>The canonical selector query still owns conflict discovery and policy. This
 * helper only consumes the cheap input-fingerprint index written after an
 * explicit/canonical player preference was already established. Every restored
 * recipe is revalidated against the live context before it can become active.</p>
 */
public final class CraftingPreferenceSeeder {

    private static final Map<Container, WeakReference<SelectionContext>> CONTEXT_CACHE =
            new WeakHashMap<Container, WeakReference<SelectionContext>>();
    private static final Map<Container, Boolean> CONTEXT_MISSES =
            new WeakHashMap<Container, Boolean>();
    private static final Map<Container, String> LAST_INPUT =
            new WeakHashMap<Container, String>();
    private static final Map<Container, ProvisionalSelection> PROVISIONAL_SELECTIONS =
            new WeakHashMap<Container, ProvisionalSelection>();

    private CraftingPreferenceSeeder() {
    }

    /**
     * Fast path used from InventoryCrafting#setInventorySlotContents before the
     * owning container performs its normal result recalculation. No option scan
     * and no nested container refresh occurs here.
     */
    public static void preseed(InventoryCrafting matrix, Container container) {
        if (matrix == null || container == null) {
            return;
        }
        if (!isMatrixOwnedBy(container, matrix)) {
            return;
        }

        SelectionContext context = detect(container);
        if (!(context instanceof CraftingContext)
                || !context.getPersistencePolicy().supportsPlayerPreferences()) {
            return;
        }

        if (((CraftingContext) context).getRecipeMatrix() != matrix) {
            return;
        }

        EntityPlayerMP player = resolvePlayer(container, context);
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }

        String inputFingerprint = InputFingerprint.create(context);
        rememberInput(container, inputFingerprint);
        ResourceLocation preferred = resolveForgePreference(
                player.world,
                player.getEntityData(),
                context,
                matrix,
                inputFingerprint);
        if (preferred != null) {
            RecipeSelectionSeeder.seed(matrix, preferred);
            markProvisional(context, preferred.toString());
        }
    }

    /**
     * Generic server-container fallback for dedicated/modded crafting contexts
     * whose visible inputs are not backed by the InventoryCrafting that performs
     * native recipe resolution (AE2 terminals are the main example).
     */
    public static void preseed(EntityPlayerMP player, Container container) {
        if (player == null
                || container == null
                || player.world == null
                || player.world.isRemote) {
            return;
        }

        SelectionContext context = detect(container);
        if (context != null) {
            preseed(player, context);
        }
    }

    static void preseed(
            @Nullable World world,
            @Nullable NBTTagCompound playerData,
            SelectionContext context) {
        if (context == null
                || playerData == null
                || !context.getPersistencePolicy().supportsPlayerPreferences()) {
            return;
        }

        String inputFingerprint = InputFingerprint.create(context);
        if (inputFingerprint == null) {
            return;
        }
        String current = SelectionContextGuard.selected(context);
        if (current != null
                && !context.getPersistencePolicy().playerPreferenceOverridesCurrent()) {
            if (!(context instanceof RecipeSelectionContext) || world == null
                    || isCurrentSelectionValid(context, current, world)) {
                return;
            }
        }

        for (String key : InputPreferenceKeys.lookupKeys(context)) {
            String preferred = PlayerRecipePreferences.lookupInput(playerData, key);
            if (preferred == null) {
                continue;
            }
            if (preferred.equals(current)) {
                return;
            }
            if (SelectionContextGuard.select(context, preferred, world)) {
                markProvisional(context, preferred);
                return;
            }
            PlayerRecipePreferences.forgetInput(playerData, key);
        }
    }

    private static void preseed(EntityPlayerMP player, SelectionContext context) {
        SelectionPersistencePolicy persistence = context.getPersistencePolicy();
        if (!persistence.supportsPlayerPreferences()) {
            return;
        }

        String inputFingerprint = InputFingerprint.create(context);
        if (inputFingerprint == null) {
            rememberInput(context.getContainer(), null);
            return;
        }

        Container container = context.getContainer();
        String current = SelectionContextGuard.selected(context);
        String previousInput = lastInput(container);
        if (inputFingerprint.equals(previousInput)
                && current != null
                && !persistence.playerPreferenceOverridesCurrent()) {
            return;
        }
        rememberInput(container, inputFingerprint);

        if (current != null
                && !persistence.playerPreferenceOverridesCurrent()
                && isCurrentSelectionValid(context, current, player.world)) {
            return;
        }

        for (String key : InputPreferenceKeys.lookupKeys(context)) {
            String preferred = PlayerRecipePreferences.lookupInput(player.getEntityData(), key);
            if (preferred == null) {
                continue;
            }
            if (preferred.equals(current)) {
                return;
            }
            if (SelectionContextGuard.select(context, preferred, player.world)) {
                markProvisional(context, preferred);
                return;
            }
            // Input aliases are only accelerators. Invalid aliases are dropped
            // independently so a broader shapeless alias can still be tried.
            PlayerRecipePreferences.forgetInput(player.getEntityData(), key);
        }
    }

    @Nullable
    private static ResourceLocation resolveForgePreference(
            World world,
            NBTTagCompound playerData,
            SelectionContext context,
            InventoryCrafting matrix,
            @Nullable String inputFingerprint) {
        if (inputFingerprint == null) {
            return null;
        }

        String current = SelectionContextGuard.selected(context);
        SelectionPersistencePolicy persistence = context.getPersistencePolicy();
        if (current != null && !persistence.playerPreferenceOverridesCurrent()) {
            ResourceLocation currentId = RecipeKey.parseForgeId(current);
            IRecipe currentRecipe = currentId == null ? null : ForgeRegistries.RECIPES.getValue(currentId);
            if (currentRecipe != null && RecipeProbe.matches(currentRecipe, matrix, world)) {
                return null;
            }
        }

        for (String key : InputPreferenceKeys.lookupKeys(context)) {
            String preferredKey = PlayerRecipePreferences.lookupInput(playerData, key);
            if (preferredKey == null) {
                continue;
            }
            ResourceLocation preferredId = RecipeKey.parseForgeId(preferredKey);
            if (preferredId == null) {
                PlayerRecipePreferences.forgetInput(playerData, key);
                continue;
            }

            IRecipe preferredRecipe = ForgeRegistries.RECIPES.getValue(preferredId);
            if (preferredRecipe != null && RecipeProbe.matches(preferredRecipe, matrix, world)) {
                return preferredId;
            }
            PlayerRecipePreferences.forgetInput(playerData, key);
        }
        return null;
    }

    private static boolean isCurrentSelectionValid(
            SelectionContext context,
            String current,
            World world) {
        if (!(context instanceof RecipeSelectionContext)) {
            // Opaque machine recipe keys cannot be validated without asking the
            // owning engine for its full option set. Preserve them and let the
            // normal query reconcile stale state.
            return true;
        }

        ResourceLocation currentId = RecipeKey.parseForgeId(current);
        IRecipe recipe = currentId == null ? null : ForgeRegistries.RECIPES.getValue(currentId);
        if (recipe == null) {
            return false;
        }
        InventoryCrafting matrix = ((RecipeSelectionContext) context).getRecipeMatrix();
        return RecipeProbe.matches(recipe, matrix, world);
    }

    @Nullable
    private static EntityPlayerMP resolvePlayer(
            Container container,
            @Nullable SelectionContext context) {
        Slot result = context == null ? null : context.getResultSlot();
        if (result instanceof SlotCraftingAccessor) {
            EntityPlayer player = ((SlotCraftingAccessor) result).retropolymorph$getPlayer();
            return player instanceof EntityPlayerMP ? (EntityPlayerMP) player : null;
        }
        return null;
    }

    @Nullable
    private static synchronized SelectionContext detect(Container container) {
        if (CONTEXT_MISSES.containsKey(container)) {
            return null;
        }
        WeakReference<SelectionContext> reference = CONTEXT_CACHE.get(container);
        SelectionContext cached = reference == null ? null : reference.get();
        if (cached != null) {
            return cached;
        }
        AdapterDetectionResult result = SelectionContextDetector.probe(container);
        SelectionContext detected = result.getContext();
        if (detected != null) {
            CONTEXT_CACHE.put(container, new WeakReference<SelectionContext>(detected));
            CONTEXT_MISSES.remove(container);
        } else if (result.isMiss() && reference != null) {
            CONTEXT_CACHE.remove(container);
            CONTEXT_MISSES.put(container, Boolean.TRUE);
        } else if (result.isMiss()) {
            CONTEXT_MISSES.put(container, Boolean.TRUE);
        }
        return detected;
    }

    static boolean isMatrixOwnedBy(Container container, InventoryCrafting matrix) {
        if (container == null || matrix == null) {
            return false;
        }
        for (Slot slot : container.inventorySlots) {
            if (slot != null && slot.inventory == matrix) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static synchronized String lastInput(Container container) {
        return container == null ? null : LAST_INPUT.get(container);
    }

    private static synchronized void rememberInput(Container container, @Nullable String fingerprint) {
        if (container == null) {
            return;
        }
        if (fingerprint == null) {
            LAST_INPUT.remove(container);
        } else {
            LAST_INPUT.put(container, fingerprint);
        }
    }

    public static synchronized void onContainerClosed(Container container) {
        if (container != null) {
            CONTEXT_CACHE.remove(container);
            CONTEXT_MISSES.remove(container);
            LAST_INPUT.remove(container);
            PROVISIONAL_SELECTIONS.remove(container);
        }
    }

    static synchronized void markProvisional(SelectionContext context, String recipeKey) {
        if (context == null || !RecipeKey.isWireSafe(recipeKey)) {
            return;
        }
        Container container = context.getContainer();
        String inputFingerprint = InputFingerprint.create(context);
        if (container != null && inputFingerprint != null) {
            PROVISIONAL_SELECTIONS.put(
                    container, new ProvisionalSelection(recipeKey, inputFingerprint));
        }
    }

    static synchronized boolean isProvisional(SelectionContext context, String recipeKey) {
        if (context == null || recipeKey == null) {
            return false;
        }
        Container container = context.getContainer();
        ProvisionalSelection state = container == null ? null : PROVISIONAL_SELECTIONS.get(container);
        return state != null
                && recipeKey.equals(state.recipeKey)
                && state.inputFingerprint.equals(InputFingerprint.create(context));
    }

    static synchronized void clearProvisional(SelectionContext context) {
        if (context != null && context.getContainer() != null) {
            PROVISIONAL_SELECTIONS.remove(context.getContainer());
        }
    }

    public static synchronized void reset() {
        CONTEXT_CACHE.clear();
        CONTEXT_MISSES.clear();
        LAST_INPUT.clear();
        PROVISIONAL_SELECTIONS.clear();
    }

    private static final class ProvisionalSelection {
        private final String recipeKey;
        private final String inputFingerprint;

        private ProvisionalSelection(String recipeKey, String inputFingerprint) {
            this.recipeKey = recipeKey;
            this.inputFingerprint = inputFingerprint;
        }
    }
}
