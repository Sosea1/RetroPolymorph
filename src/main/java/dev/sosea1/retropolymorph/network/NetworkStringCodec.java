package dev.sosea1.retropolymorph.network;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Small bounded UTF-8 codec for network-controlled identifiers.
 *
 * Length uses the same two-byte VarInt form as Forge's UTF-8 helpers, but the
 * decoder is deliberately non-throwing for truncated or oversized input.
 */
final class NetworkStringCodec {

    private NetworkStringCodec() {
    }

    @Nullable
    static String read(ByteBuf buf, int maxBytes) {
        if (!buf.isReadable()) {
            return null;
        }

        int first = buf.readUnsignedByte();
        int byteLength = first & 0x7F;
        if ((first & 0x80) != 0) {
            if (!buf.isReadable()) {
                return null;
            }

            int second = buf.readUnsignedByte();
            if ((second & 0x80) != 0) {
                return null;
            }
            byteLength |= (second & 0x7F) << 7;
        }

        if (byteLength <= 0 || byteLength > maxBytes || byteLength > buf.readableBytes()) {
            return null;
        }

        byte[] bytes = new byte[byteLength];
        buf.getBytes(buf.readerIndex(), bytes);
        String value;
        try {
            value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException malformedUtf8) {
            return null;
        }
        buf.skipBytes(byteLength);
        return value;
    }

    static void write(ByteBuf buf, String value, int maxBytes) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int byteLength = bytes.length;
        if (byteLength == 0 || byteLength > maxBytes || byteLength > 0x3FFF) {
            throw new IllegalArgumentException("Encoded string length is invalid: " + byteLength);
        }

        if (byteLength < 0x80) {
            buf.writeByte(byteLength);
        } else {
            buf.writeByte((byteLength & 0x7F) | 0x80);
            buf.writeByte(byteLength >>> 7);
        }
        buf.writeBytes(bytes);
    }
}
