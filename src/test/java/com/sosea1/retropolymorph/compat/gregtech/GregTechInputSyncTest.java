package com.sosea1.retropolymorph.compat.gregtech;

import com.sosea1.retropolymorph.mixin.compat.gregtech.GregTechInputSyncHandlerMixin;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GregTechInputSyncTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test
    void serverSlotChangeStillReachesNativeChangeDetection() throws Exception {
        ServerInput handler = new ServerInput();
        handler.setSnapshot(ItemStack.EMPTY);
        handler.setStack(new ItemStack(Blocks.DIRT));
        assertTrue(handler.needsUpdate(),
                "A server slot write must not acknowledge its own unsent change");
    }

    @Test
    void serverSlotClearStillReachesNativeChangeDetection() throws Exception {
        ServerInput handler = new ServerInput();
        handler.setSnapshot(new ItemStack(Blocks.DIRT));
        handler.setStack(ItemStack.EMPTY);
        assertTrue(handler.needsUpdate(),
                "Clearing a ghost slot must remain visible to the server's next sync pass");
    }

    private static final class ServerInput extends GregTechInputSyncHandlerMixin {
        private ItemStack stack = ItemStack.EMPTY;

        public ItemStack getStack() { return stack; }

        void setStack(ItemStack value) throws Exception {
            stack = value;
            // Execute the production hooks registered on the native server setStack boundary.
            for (Method method : GregTechInputSyncHandlerMixin.class.getDeclaredMethods()) {
                Inject injection = method.getAnnotation(Inject.class);
                if (injection != null && Arrays.asList(injection.method()).contains("setStack")) {
                    method.setAccessible(true);
                    method.invoke(this, value, false, new CallbackInfo("setStack", false));
                }
            }
        }

        void setSnapshot(ItemStack value) throws Exception { snapshotField().set(this, value); }

        boolean needsUpdate() throws Exception {
            ItemStack previous = (ItemStack) snapshotField().get(this);
            if (stack.isEmpty() && previous.isEmpty()) return false;
            return !ItemHandlerHelper.canItemStacksStack(previous, stack);
        }

        private static Field snapshotField() throws Exception {
            Field field = GregTechInputSyncHandlerMixin.class.getDeclaredField("lastStoredItem");
            field.setAccessible(true);
            return field;
        }
    }
}
