package dev.sosea1.retropolymorph.preference;

import dev.sosea1.retropolymorph.api.RecipeSelectionContext;
import dev.sosea1.retropolymorph.api.SelectionContext;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cheap identities for the visible inputs of one selection surface.
 *
 * <p>The positional crafting key trims empty border rows/columns, so translating
 * a recipe around the crafting grid does not change its identity. It deliberately
 * preserves the relative occupied shape. Horizontal mirroring is represented by
 * a separate key and is only persisted when the selected recipe actually matches
 * the mirrored layout.</p>
 *
 * <p>The unordered key ignores slot positions completely. It is only persisted
 * for recipes with known shapeless semantics, but can always be computed during
 * lookup. This lets normal shapeless recipes keep one remembered choice across
 * arbitrary slot permutations without collapsing shaped recipes onto the same key.</p>
 *
 * <p>Stack count is deliberately ignored. Every accelerated lookup is still
 * revalidated against the live recipe/context before it can become authoritative.</p>
 */
public final class InputFingerprint {

    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private static final String POSITIONAL_PREFIX = "i3:p:";
    private static final String UNORDERED_PREFIX = "i3:u:";
    private static final String FORGE_CRAFTING_DOMAIN = "forge_crafting";

    private InputFingerprint() {
    }

    /** Translation-normalized, shape-preserving identity. */
    @Nullable
    public static String create(@Nullable SelectionContext context) {
        if (context == null) {
            return null;
        }

        int inputCount = context.getInputCount();
        if (inputCount <= 0) {
            return null;
        }

        long hash = baseHash(context);
        if (context instanceof RecipeSelectionContext) {
            InventoryCrafting matrix = ((RecipeSelectionContext) context).getRecipeMatrix();
            String normalized = createNormalizedGrid(context, matrix, hash, inputCount, false);
            if (normalized != null) {
                return normalized;
            }
        }
        return createExactSlots(context, hash, inputCount);
    }

    /**
     * Translation-normalized identity of the same occupied shape reflected on
     * the horizontal axis. Callers must not assume the owning recipe accepts it.
     */
    @Nullable
    public static String createHorizontalMirror(@Nullable SelectionContext context) {
        if (!(context instanceof RecipeSelectionContext)) {
            return null;
        }
        int inputCount = context.getInputCount();
        if (inputCount <= 0) {
            return null;
        }
        InventoryCrafting matrix = ((RecipeSelectionContext) context).getRecipeMatrix();
        return createNormalizedGrid(context, matrix, baseHash(context), inputCount, true);
    }

    /**
     * Position-independent multiset identity. Stored only for known shapeless
     * recipes; lookup may compute it unconditionally because absent keys are free.
     */
    @Nullable
    public static String createUnordered(@Nullable SelectionContext context) {
        if (context == null || context.getInputCount() <= 0) {
            return null;
        }

        List<StackIdentity> stacks = new ArrayList<StackIdentity>(context.getInputCount());
        for (int index = 0; index < context.getInputCount(); index++) {
            ItemStack stack = context.getInputStack(index);
            if (stack != null && !stack.isEmpty()) {
                stacks.add(StackIdentity.of(stack));
            }
        }
        if (stacks.isEmpty()) {
            return null;
        }
        Collections.sort(stacks);

        long hash = baseHash(context);
        hash = mixInt(hash, 0x554E4F52); // UNOR
        hash = addGridDimensions(context, hash);
        hash = mixInt(hash, stacks.size());
        for (StackIdentity stack : stacks) {
            hash = mixString(hash, stack.itemId);
            hash = mixInt(hash, stack.metadata);
            hash = mixInt(hash, stack.tagHash);
        }
        return UNORDERED_PREFIX + Long.toHexString(hash);
    }

    @Nullable
    static InventoryCrafting createHorizontalMirrorMatrix(@Nullable RecipeSelectionContext context) {
        if (context == null) {
            return null;
        }
        InventoryCrafting source = context.getRecipeMatrix();
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return null;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int[] bounds = occupiedBounds(context, width, height);
        if (bounds == null) {
            return null;
        }

        InventoryCrafting mirror = new InventoryCrafting(new NoopContainer(), width, height);
        int minX = bounds[0];
        int minY = bounds[1];
        int maxX = bounds[2];
        int maxY = bounds[3];
        int occupiedWidth = maxX - minX + 1;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int sourceIndex = y * width + x;
                ItemStack stack = context.getInputStack(sourceIndex);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                int relativeX = x - minX;
                int mirroredX = minX + (occupiedWidth - relativeX - 1);
                mirror.setInventorySlotContents(y * width + mirroredX, stack.copy());
            }
        }
        return mirror;
    }

    private static long baseHash(SelectionContext context) {
        long hash = FNV_OFFSET_BASIS;
        hash = mixString(hash, context instanceof RecipeSelectionContext
                ? FORGE_CRAFTING_DOMAIN
                : context.getClass().getName());
        return mixInt(hash, context.getClientStateToken());
    }

    @Nullable
    private static String createNormalizedGrid(
            SelectionContext context,
            @Nullable InventoryCrafting matrix,
            long baseHash,
            int inputCount,
            boolean mirror) {
        if (matrix == null) {
            return null;
        }

        int width = matrix.getWidth();
        int height = matrix.getHeight();
        if (width <= 0 || height <= 0 || inputCount < width * height) {
            return null;
        }

        int[] bounds = occupiedBounds(context, width, height);
        if (bounds == null) {
            return null;
        }
        int minX = bounds[0];
        int minY = bounds[1];
        int maxX = bounds[2];
        int maxY = bounds[3];
        int occupiedWidth = maxX - minX + 1;
        int occupiedHeight = maxY - minY + 1;

        long hash = baseHash;
        hash = mixInt(hash, 0x47524944); // GRID
        hash = mixInt(hash, width);
        hash = mixInt(hash, height);
        hash = mixInt(hash, occupiedWidth);
        hash = mixInt(hash, occupiedHeight);

        for (int relativeY = 0; relativeY < occupiedHeight; relativeY++) {
            for (int relativeX = 0; relativeX < occupiedWidth; relativeX++) {
                int sourceRelativeX = mirror ? occupiedWidth - relativeX - 1 : relativeX;
                int sourceX = minX + sourceRelativeX;
                int sourceY = minY + relativeY;
                hash = mixStack(hash, context.getInputStack(sourceY * width + sourceX));
            }
        }

        return POSITIONAL_PREFIX + Long.toHexString(hash);
    }

    @Nullable
    private static int[] occupiedBounds(SelectionContext context, int width, int height) {
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        int gridSlots = width * height;
        for (int index = 0; index < gridSlots; index++) {
            ItemStack stack = context.getInputStack(index);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int x = index % width;
            int y = index / width;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        return maxX < minX || maxY < minY ? null : new int[] { minX, minY, maxX, maxY };
    }

    @Nullable
    private static String createExactSlots(SelectionContext context, long baseHash, int inputCount) {
        long hash = baseHash;
        hash = mixInt(hash, 0x534C4F54); // SLOT
        hash = mixInt(hash, inputCount);

        boolean anyInput = false;
        for (int index = 0; index < inputCount; index++) {
            hash = mixInt(hash, index);
            ItemStack stack = context.getInputStack(index);
            if (stack != null && !stack.isEmpty()) {
                anyInput = true;
            }
            hash = mixStack(hash, stack);
        }
        return anyInput ? POSITIONAL_PREFIX + Long.toHexString(hash) : null;
    }

    private static long addGridDimensions(SelectionContext context, long hash) {
        if (context instanceof RecipeSelectionContext) {
            InventoryCrafting matrix = ((RecipeSelectionContext) context).getRecipeMatrix();
            if (matrix != null) {
                hash = mixInt(hash, matrix.getWidth());
                hash = mixInt(hash, matrix.getHeight());
                return hash;
            }
        }
        return mixInt(hash, context.getInputCount());
    }

    private static long mixStack(long hash, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return mixInt(hash, 0);
        }
        StackIdentity identity = StackIdentity.of(stack);
        hash = mixInt(hash, 1);
        hash = mixString(hash, identity.itemId);
        hash = mixInt(hash, identity.metadata);
        return mixInt(hash, identity.tagHash);
    }

    private static long mixString(long hash, String value) {
        if (value == null) {
            return mixInt(hash, 0);
        }
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= FNV_PRIME;
        }
        return mixInt(hash, value.length());
    }

    private static long mixInt(long hash, int value) {
        hash ^= value & 0xFFL;
        hash *= FNV_PRIME;
        hash ^= (value >>> 8) & 0xFFL;
        hash *= FNV_PRIME;
        hash ^= (value >>> 16) & 0xFFL;
        hash *= FNV_PRIME;
        hash ^= (value >>> 24) & 0xFFL;
        hash *= FNV_PRIME;
        return hash;
    }

    private static final class StackIdentity implements Comparable<StackIdentity> {
        private final String itemId;
        private final int metadata;
        private final int tagHash;

        private StackIdentity(String itemId, int metadata, int tagHash) {
            this.itemId = itemId;
            this.metadata = metadata;
            this.tagHash = tagHash;
        }

        private static StackIdentity of(ItemStack stack) {
            ResourceLocation itemId = stack.getItem().getRegistryName();
            NBTTagCompound tag = stack.getTagCompound();
            return new StackIdentity(
                    itemId == null ? "<unregistered>" : itemId.toString(),
                    stack.getMetadata(),
                    tag == null ? 0 : tag.hashCode());
        }

        @Override
        public int compareTo(StackIdentity other) {
            int byItem = this.itemId.compareTo(other.itemId);
            if (byItem != 0) return byItem;
            int byMeta = Integer.compare(this.metadata, other.metadata);
            if (byMeta != 0) return byMeta;
            return Integer.compare(this.tagHash, other.tagHash);
        }
    }

    private static final class NoopContainer extends net.minecraft.inventory.Container {
        @Override
        public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) {
        }

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return false;
        }
    }
}
