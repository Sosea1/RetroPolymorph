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
        invokeNbt(saved, "retropolymorph$writeSelection", tag);
        return tag;
    }
    private static void read(SavedRecipe saved, NBTTagCompound tag) throws Exception {
        invokeNbt(saved, "retropolymorph$readSelection", tag);
    }
    private static void invokeNbt(SavedRecipe saved, String name, NBTTagCompound tag) throws Exception {
        Method method = RFToolsCraftingRecipeMixin.class.getDeclaredMethod(name, NBTTagCompound.class, CallbackInfo.class);
        method.setAccessible(true);
        method.invoke(saved, tag, new CallbackInfo(name, false));
    }

    // Supplies the native fallback while testing the actual mixin's selection and NBT hooks.
    private static class SavedRecipe extends RFToolsCraftingRecipeMixin {
        private final InventoryCrafting matrix;
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
            return CraftingManager.findMatchingRecipe(this.matrix, world);
        }
    }
    private static class TestWorld extends World {
        TestWorld() { super(null, null, null, null, false); }
        protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() { return null; }
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
    }
}
