package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeOption;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RecipeSelectionSyncMessageTest {

    private static Item testItem;

    @BeforeAll
    public static void setup() {
        Bootstrap.register();
        testItem = Items.IRON_INGOT;
    }

    @Test
    public void testSyncMessageEncodeDecodeWithSelectionAndOptions() {
        List<RecipeOption> options = new ArrayList<RecipeOption>();
        options.add(new RecipeOption("minecraft:iron_block", new ItemStack(testItem)));
        options.add(new RecipeOption("othermod:iron_block_alt", new ItemStack(testItem)));

        RecipeSelectionSyncMessage msg = new RecipeSelectionSyncMessage(
                12, 345, 6, true, "minecraft:iron_block", options);

        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);

        RecipeSelectionSyncMessage decoded = new RecipeSelectionSyncMessage();
        decoded.fromBytes(buf);

        assertTrue(decoded.isValid());
        assertEquals(12, decoded.getWindowId());
        assertEquals(345, decoded.getSessionToken());
        assertEquals(6, decoded.getInputRevision());
        assertTrue(decoded.isAccepted());
        assertEquals("minecraft:iron_block", decoded.getSelectedRecipeKey());
        assertEquals(2, decoded.getOptions().size());
        assertEquals("minecraft:iron_block", decoded.getOptions().get(0).getRecipeKey());
        assertEquals("othermod:iron_block_alt", decoded.getOptions().get(1).getRecipeKey());
    }

    @Test
    public void testSyncMessageWithoutSelectionOrOptions() {
        RecipeSelectionSyncMessage msg = new RecipeSelectionSyncMessage(
                1, 2, 3, false, null, Collections.<RecipeOption>emptyList());

        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);

        RecipeSelectionSyncMessage decoded = new RecipeSelectionSyncMessage();
        decoded.fromBytes(buf);

        assertTrue(decoded.isValid());
        assertEquals(1, decoded.getWindowId());
        assertEquals(2, decoded.getSessionToken());
        assertEquals(3, decoded.getInputRevision());
        assertFalse(decoded.isAccepted());
        assertNull(decoded.getSelectedRecipeKey());
        assertTrue(decoded.getOptions().isEmpty());
    }

    @Test
    public void testTruncatedPacketIsInvalid() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1); // truncated before windowId/session/rev/accepted
        RecipeSelectionSyncMessage decoded = new RecipeSelectionSyncMessage();
        decoded.fromBytes(buf);

        assertFalse(decoded.isValid());
    }

    @Test
    public void testTrailingBytesInvalidateMessage() {
        RecipeSelectionSyncMessage msg = new RecipeSelectionSyncMessage(
                1, 2, 3, false, null, Collections.<RecipeOption>emptyList());
        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);
        buf.writeByte(0xAA); // trailing garbage byte

        RecipeSelectionSyncMessage decoded = new RecipeSelectionSyncMessage();
        decoded.fromBytes(buf);

        assertFalse(decoded.isValid());
    }

    @Test
    public void testSyncMessageWithSelectionReason() {
        List<RecipeOption> options = Collections.singletonList(
                new RecipeOption("minecraft:iron_block", new ItemStack(testItem)));

        RecipeSelectionSyncMessage msg = new RecipeSelectionSyncMessage(
                12, 345, 6, true, "minecraft:iron_block", options, dev.sosea1.retropolymorph.api.SelectionReason.PLAYER_PREFERENCE);

        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);

        RecipeSelectionSyncMessage decoded = new RecipeSelectionSyncMessage();
        decoded.fromBytes(buf);

        assertTrue(decoded.isValid());
        assertEquals(dev.sosea1.retropolymorph.api.SelectionReason.PLAYER_PREFERENCE, decoded.getReason());
    }

    @Test
    public void testUnknownSelectionReasonOrdinalFallsBackToNativeDefault() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(10); // windowId
        buf.writeInt(20); // sessionToken
        buf.writeInt(30); // inputRevision
        buf.writeBoolean(true); // accepted
        buf.writeByte(99); // unknown reason wireId
        buf.writeBoolean(false); // hasSelection
        buf.writeByte(0); // optionCount

        RecipeSelectionSyncMessage decoded = new RecipeSelectionSyncMessage();
        decoded.fromBytes(buf);

        assertTrue(decoded.isValid());
        assertEquals(dev.sosea1.retropolymorph.api.SelectionReason.NATIVE_DEFAULT, decoded.getReason());
    }

    @Test
    public void testAllSelectionReasonsHaveStableWireIds() {
        assertEquals(0, dev.sosea1.retropolymorph.api.SelectionReason.CURRENT_CONTEXT.getWireId());
        assertEquals(1, dev.sosea1.retropolymorph.api.SelectionReason.PLAYER_PREFERENCE.getWireId());
        assertEquals(2, dev.sosea1.retropolymorph.api.SelectionReason.EXACT_RECIPE_POLICY.getWireId());
        assertEquals(3, dev.sosea1.retropolymorph.api.SelectionReason.MOD_PRIORITY.getWireId());
        assertEquals(4, dev.sosea1.retropolymorph.api.SelectionReason.AUTOMATIC_MODDED.getWireId());
        assertEquals(5, dev.sosea1.retropolymorph.api.SelectionReason.NATIVE_DEFAULT.getWireId());
        assertEquals(6, dev.sosea1.retropolymorph.api.SelectionReason.PLAYER_SELECTION.getWireId());

        for (dev.sosea1.retropolymorph.api.SelectionReason reason : dev.sosea1.retropolymorph.api.SelectionReason.values()) {
            assertEquals(reason, dev.sosea1.retropolymorph.api.SelectionReason.fromWireId(reason.getWireId()));
        }
    }
}