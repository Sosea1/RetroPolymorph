package com.sosea1.retropolymorph.compat.rftools;

import com.sosea1.retropolymorph.mixin.compat.rftools.RFToolsCraftingRecipeMixin;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class RFToolsSavedRecipeTest {
    private static IRecipe diamond;
    private static IRecipe emerald;
    private static World world;

    @BeforeAll
    static void bootstrap() throws Exception {
        Bootstrap.register();
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        world = (World) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(TestWorld.class);
        diamond = recipe("rftools_default", new ItemStack(Items.DIAMOND));
        emerald = recipe("rftools_selected", new ItemStack(Items.EMERALD));
        ForgeRegistry<IRecipe> registry = (ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES;
        registry.unfreeze();
        try { registry.register(diamond); registry.register(emerald); }
        finally { registry.freeze(); }
    }

    @Test
    void authoritativeChoiceControlsCachedRecipeAndPreview() throws Exception {
        SavedRecipe saved = new SavedRecipe();
        assertSame(diamond, CraftingManager.findMatchingRecipe(saved.matrix, world));
        saved.retropolymorph$setSelectedRecipeId(emerald.getRegistryName(), world);
        assertSame(emerald, resolvedByHook(saved));
        assertEquals(Items.EMERALD, saved.retropolymorph$getOutput().getItem());
    }

    @Test
    void choicesPersistSeparatelyForEachSavedTemplate() throws Exception {
        SavedRecipe first = new SavedRecipe();
        SavedRecipe second = new SavedRecipe();
        first.retropolymorph$setSelectedRecipeId(emerald.getRegistryName(), world);
        second.retropolymorph$setSelectedRecipeId(diamond.getRegistryName(), world);
        SavedRecipe loadedFirst = new SavedRecipe();
        SavedRecipe loadedSecond = new SavedRecipe();
        read(loadedFirst, write(first));
        read(loadedSecond, write(second));
        assertSame(emerald, resolvedByHook(loadedFirst));
        assertSame(diamond, resolvedByHook(loadedSecond));
    }

    @Test
    void invalidChoiceFallsBackToNativeRecipeAndRecomputesOutput() throws Exception {
        SavedRecipe saved = new SavedRecipe();
        saved.retropolymorph$setSelectedRecipeId(emerald.getRegistryName(), world);
        saved.retropolymorph$setSelectedRecipeId(new ResourceLocation("test", "missing_recipe"), world);
        assertNull(saved.retropolymorph$getSelectedRecipeId());
        assertEquals(Items.DIAMOND, saved.retropolymorph$getOutput().getItem());
    }

    @Test
    void changedInputsCannotUseStoredChoice() throws Exception {
        SavedRecipe saved = new SavedRecipe();
        saved.retropolymorph$setSelectedRecipeId(emerald.getRegistryName(), world);
        saved.matrix.clear();
        assertNull(resolvedByHook(saved));
        assertNull(saved.retropolymorph$getSelectedRecipeId());
    }

    @Test
    void loadedMissingChoiceRefreshesPreviewFromNativeFallbackOnlyOnce() throws Exception {
        SavedRecipe saved = new SavedRecipe();
        saved.retropolymorph$setSelectedRecipeId(emerald.getRegistryName(), world);
        NBTTagCompound tag = write(saved);
        tag.setString("RetroPolymorphRecipe", "test:missing_recipe");
        SavedRecipe loaded = new SavedRecipe();
        read(loaded, tag);
        assertEquals(Items.EMERALD, loaded.retropolymorph$getOutput().getItem());

        assertSame(diamond, loaded.getCachedRecipe(world));
        assertNull(loaded.retropolymorph$getSelectedRecipeId());
        assertEquals(Items.DIAMOND, loaded.retropolymorph$getOutput().getItem());
        ItemStack refreshed = loaded.retropolymorph$getOutput();
        assertSame(diamond, loaded.getCachedRecipe(world));
        assertSame(refreshed, loaded.retropolymorph$getOutput());
        assertEquals(1, loaded.nativeLookups);
    }

    @Test
    void loadedChoiceWithNonMatchingInputsClearsPreviewWhenNoFallbackExists() throws Exception {
        SavedRecipe saved = new SavedRecipe();
        saved.retropolymorph$setSelectedRecipeId(emerald.getRegistryName(), world);
        SavedRecipe loaded = new SavedRecipe();
        read(loaded, write(saved));
        loaded.matrix.clear();
        assertEquals(Items.EMERALD, loaded.retropolymorph$getOutput().getItem());

        assertNull(loaded.getCachedRecipe(world));
        assertNull(loaded.retropolymorph$getSelectedRecipeId());
        assertTrue(loaded.retropolymorph$getOutput().isEmpty());
        assertNull(loaded.getCachedRecipe(world));
        assertEquals(1, loaded.nativeLookups);
    }

    private static IRecipe recipe(String path, ItemStack output) {
        return new ShapelessRecipes("", output, NonNullList.from(Ingredient.EMPTY,
                Ingredient.fromStacks(new ItemStack(Blocks.DIRT))))
                .setRegistryName("retropolymorph_test", path);
    }

    private static IRecipe resolvedByHook(SavedRecipe saved) throws Exception {
        CallbackInfoReturnable<IRecipe> result = new CallbackInfoReturnable<>("getCachedRecipe", true);
        Method method = RFToolsCraftingRecipeMixin.class.getDeclaredMethod("retropolymorph$resolveSelected", World.class, CallbackInfoReturnable.class);
        method.setAccessible(true);
        method.invoke(saved, world, result);
        return result.getReturnValue();
    }

    private static NBTTagCompound write(SavedRecipe saved) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Result", saved.retropolymorph$getOutput().writeToNBT(new NBTTagCompound()));
        invokeNbt(saved, "retropolymorph$writeSelection", tag);
        return tag;
    }
    private static void read(SavedRecipe saved, NBTTagCompound tag) throws Exception {
        setField(saved, "result", new ItemStack(tag.getCompoundTag("Result")));
        invokeNbt(saved, "retropolymorph$readSelection", tag);
    }
    private static void setField(SavedRecipe saved, String name, Object value) throws ReflectiveOperationException {
        Field field = RFToolsCraftingRecipeMixin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(saved, value);
    }
    private static Object getField(SavedRecipe saved, String name) throws ReflectiveOperationException {
        Field field = RFToolsCraftingRecipeMixin.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(saved);
    }
    private static void invokeNbt(SavedRecipe saved, String name, NBTTagCompound tag) throws Exception {
        Method method = RFToolsCraftingRecipeMixin.class.getDeclaredMethod(name, NBTTagCompound.class, CallbackInfo.class);
        method.setAccessible(true);
        method.invoke(saved, tag, new CallbackInfo(name, false));
    }

    // Supplies the native fallback while testing the actual mixin's selection and NBT hooks.
    private static class SavedRecipe extends RFToolsCraftingRecipeMixin {
        private final InventoryCrafting matrix;
        private int nativeLookups;
        SavedRecipe() throws Exception {
            this.matrix = new InventoryCrafting(new net.minecraft.inventory.Container() {
                public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return false; }
            }, 3, 3);
            matrix.setInventorySlotContents(4, new ItemStack(Blocks.DIRT));
            Field field = RFToolsCraftingRecipeMixin.class.getDeclaredField("inv");
            field.setAccessible(true);
            field.set(this, matrix);
        }
        public IRecipe getCachedRecipe(World world) {
            try {
                CallbackInfoReturnable<IRecipe> callback = new CallbackInfoReturnable<>("getCachedRecipe", true);
                Method head = RFToolsCraftingRecipeMixin.class.getDeclaredMethod("retropolymorph$resolveSelected", World.class, CallbackInfoReturnable.class);
                head.setAccessible(true);
                head.invoke(this, world, callback);
                if (callback.isCancelled()) { return callback.getReturnValue(); }
                if (!(Boolean) getField(this, "recipePresent")) {
                    nativeLookups++;
                    setField(this, "recipePresent", true);
                    IRecipe fallback = null;
                    for (IRecipe candidate : CraftingManager.REGISTRY) {
                        if (candidate != null && candidate.matches(matrix, world)) { fallback = candidate; break; }
                    }
                    setField(this, "recipe", fallback);
                }
                IRecipe resolved = (IRecipe) getField(this, "recipe");
                callback.setReturnValue(resolved);
                Method tail = RFToolsCraftingRecipeMixin.class.getDeclaredMethod("retropolymorph$refreshFallbackResult", World.class, CallbackInfoReturnable.class);
                tail.setAccessible(true);
                tail.invoke(this, world, callback);
                return resolved;
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        }
    }
    private static class TestWorld extends World {
        TestWorld() { super(null, null, null, null, false); }
        protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() { return null; }
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
    }
}
