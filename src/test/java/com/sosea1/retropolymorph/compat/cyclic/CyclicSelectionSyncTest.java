package com.sosea1.retropolymorph.compat.cyclic;

import com.sosea1.retropolymorph.config.PolymorphConfig;
import com.sosea1.retropolymorph.mixin.compat.cyclic.CyclicTileCrafterMixin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

final class CyclicSelectionSyncTest {
    private boolean previousRemember;
    private boolean previousIntegration;

    @BeforeAll
    static void bootstrap() { Bootstrap.register(); }

    @BeforeEach
    void disablePersistence() throws Exception {
        previousRemember = setConfig("rememberPlayerChoices", false);
        previousIntegration = setConfig("integrationCyclic", true);
    }

    @AfterEach
    void restoreConfig() throws Exception {
        setConfig("rememberPlayerChoices", previousRemember);
        setConfig("integrationCyclic", previousIntegration);
    }

    @Test
    void repeatedTileUpdatesKeepCurrentChoiceWithPersistenceDisabled() throws Exception {
        Crafter server = new Crafter();
        Crafter client = new Crafter();
        ResourceLocation emerald = new ResourceLocation("test", "emerald");
        server.retropolymorph$setSelectedRecipeId(emerald);
        client.retropolymorph$setSelectedRecipeId(emerald);

        for (int update = 0; update < 3; update++) {
            NBTTagCompound packet = diskTag(server);
            readTag(client, packet);
            assertEquals(emerald, client.retropolymorph$getSelectedRecipeId());
        }
    }

    @Test
    void machineRecipePreservedInNBTWhenRememberPlayerChoicesIsDisabled() throws Exception {
        Crafter server = new Crafter();
        ResourceLocation emerald = new ResourceLocation("test", "emerald");
        server.retropolymorph$setSelectedRecipeId(emerald);
        NBTTagCompound saved = diskTag(server);
        assertTrue(saved.hasKey("RetroPolymorphRecipe"));
        assertTrue(saved.hasKey("RetroPolymorphTemplates"));
        Crafter loaded = new Crafter();
        readTag(loaded, saved);
        assertEquals(emerald, loaded.retropolymorph$getSelectedRecipeId());
    }

    private static NBTTagCompound diskTag(Crafter crafter) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        Method write = CyclicTileCrafterMixin.class.getDeclaredMethod("retropolymorph$writeRecipeSelection",
                NBTTagCompound.class, CallbackInfoReturnable.class);
        write.setAccessible(true);
        write.invoke(crafter, tag, new CallbackInfoReturnable<NBTTagCompound>("writeToNBT", false, tag));
        return tag;
    }

    private static void readTag(Crafter crafter, NBTTagCompound tag) throws Exception {
        Method read = CyclicTileCrafterMixin.class.getDeclaredMethod("retropolymorph$readRecipeSelection",
                NBTTagCompound.class, CallbackInfo.class);
        read.setAccessible(true);
        read.invoke(crafter, tag, new CallbackInfo("readFromNBT", false));
    }

    private static boolean setConfig(String name, boolean value) throws Exception {
        Field field = PolymorphConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        boolean previous = field.getBoolean(null);
        field.setBoolean(null, value);
        return previous;
    }

    private static final class Crafter extends CyclicTileCrafterMixin implements IInventory {
        private final InventoryBasic inventory = new InventoryBasic("crafter", false, 29);
        Crafter() { inventory.setInventorySlotContents(14, new ItemStack(Blocks.DIRT)); }
        @Override protected void findRecipe() { }
        @Override public int getSizeInventory() { return inventory.getSizeInventory(); }
        @Override public boolean isEmpty() { return inventory.isEmpty(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack decrStackSize(int slot, int count) { return inventory.decrStackSize(slot, count); }
        @Override public ItemStack removeStackFromSlot(int slot) { return inventory.removeStackFromSlot(slot); }
        @Override public void setInventorySlotContents(int slot, ItemStack stack) { inventory.setInventorySlotContents(slot, stack); }
        @Override public int getInventoryStackLimit() { return inventory.getInventoryStackLimit(); }
        @Override public void markDirty() { inventory.markDirty(); }
        @Override public boolean isUsableByPlayer(EntityPlayer player) { return true; }
        @Override public void openInventory(EntityPlayer player) { }
        @Override public void closeInventory(EntityPlayer player) { }
        @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
        @Override public int getField(int id) { return 0; }
        @Override public void setField(int id, int value) { }
        @Override public int getFieldCount() { return 0; }
        @Override public void clear() { inventory.clear(); }
        @Override public String getName() { return inventory.getName(); }
        @Override public boolean hasCustomName() { return false; }
        @Override public ITextComponent getDisplayName() { return inventory.getDisplayName(); }
    }
}
