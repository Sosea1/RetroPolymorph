package dev.sosea1.retropolymorph.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RecipeSelectionMessageTest {

    @Test
    public void testSelectMessageEncodeDecode() {
        RecipeSelectionMessage msg = RecipeSelectionMessage.select(42, 100, 5, "minecraft:iron_block");
        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);

        RecipeSelectionMessage decoded = new RecipeSelectionMessage();
        decoded.fromBytes(buf);

        assertTrue(decoded.isValid());
        assertEquals(42, decoded.getWindowId());
        assertEquals(100, decoded.getSessionToken());
        assertEquals(5, decoded.getInputRevision());
        assertTrue(decoded.isSelect());
        assertFalse(decoded.isClear());
        assertFalse(decoded.isQuery());
        assertEquals("minecraft:iron_block", decoded.getRecipeKey());
    }

    @Test
    public void testClearMessageEncodeDecode() {
        RecipeSelectionMessage msg = RecipeSelectionMessage.clear(10, 20, 3);
        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);

        RecipeSelectionMessage decoded = new RecipeSelectionMessage();
        decoded.fromBytes(buf);

        assertTrue(decoded.isValid());
        assertEquals(10, decoded.getWindowId());
        assertEquals(20, decoded.getSessionToken());
        assertEquals(3, decoded.getInputRevision());
        assertTrue(decoded.isClear());
        assertFalse(decoded.isSelect());
        assertNull(decoded.getRecipeKey());
    }

    @Test
    public void testQueryMessageEncodeDecode() {
        RecipeSelectionMessage msg = RecipeSelectionMessage.query(7, 8, 9);
        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);

        RecipeSelectionMessage decoded = new RecipeSelectionMessage();
        decoded.fromBytes(buf);

        assertTrue(decoded.isValid());
        assertEquals(7, decoded.getWindowId());
        assertEquals(8, decoded.getSessionToken());
        assertEquals(9, decoded.getInputRevision());
        assertTrue(decoded.isQuery());
        assertNull(decoded.getRecipeKey());
    }

    @Test
    public void testTruncatedPacketIsInvalid() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1); // only 4 bytes instead of minimum 13
        RecipeSelectionMessage decoded = new RecipeSelectionMessage();
        decoded.fromBytes(buf);

        assertFalse(decoded.isValid());
    }

    @Test
    public void testTrailingBytesInvalidateMessage() {
        RecipeSelectionMessage msg = RecipeSelectionMessage.clear(1, 2, 3);
        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);
        buf.writeByte(0xFF); // extra trailing garbage byte

        RecipeSelectionMessage decoded = new RecipeSelectionMessage();
        decoded.fromBytes(buf);

        assertFalse(decoded.isValid());
    }

    @Test
    public void testInvalidOperationByteInvalidatesMessage() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1); // window
        buf.writeInt(2); // session
        buf.writeInt(3); // revision
        buf.writeByte(99); // invalid operation

        RecipeSelectionMessage decoded = new RecipeSelectionMessage();
        decoded.fromBytes(buf);

        assertFalse(decoded.isValid());
    }
}
