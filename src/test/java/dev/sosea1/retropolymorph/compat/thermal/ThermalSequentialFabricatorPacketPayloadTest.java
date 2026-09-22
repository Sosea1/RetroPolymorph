package dev.sosea1.retropolymorph.compat.thermal;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSequentialFabricatorPacketPayloadTest {

    @Test
    void roundTripsSelectedRecipe() {
        ResourceLocation selected = new ResourceLocation("thermalfoundation", "gear_copper");
        ThermalSequentialFabricatorPacketPayload.Decoded decoded =
                ThermalSequentialFabricatorPacketPayload.decode(
                        ThermalSequentialFabricatorPacketPayload.encode(selected));

        assertTrue(decoded.isRecognized());
        assertEquals(selected, decoded.getRecipeId());
    }

    @Test
    void explicitFrameCanClearSelection() {
        ThermalSequentialFabricatorPacketPayload.Decoded decoded =
                ThermalSequentialFabricatorPacketPayload.decode(
                        ThermalSequentialFabricatorPacketPayload.encode(null));

        assertTrue(decoded.isRecognized());
        assertNull(decoded.getRecipeId());
    }

    @Test
    void doesNotTreatLegacyOrForeignTrailingStringsAsSelection() {
        assertFalse(ThermalSequentialFabricatorPacketPayload.decode(
                "thermalfoundation:gear_copper").isRecognized());
        assertFalse(ThermalSequentialFabricatorPacketPayload.decode(
                "othermod:some_packet_extension").isRecognized());
        assertFalse(ThermalSequentialFabricatorPacketPayload.decode(null).isRecognized());
    }

    @Test
    void rejectsWireUnsafeFramedRecipeIds() {
        assertFalse(ThermalSequentialFabricatorPacketPayload.decode(
                "retropolymorph:thermal_selection:v1:minecraft:bad\ncontrol").isRecognized());
    }

    @Test
    void doesNotImposeModernResourceLocationRegexOnLegacyIds() {
        ThermalSequentialFabricatorPacketPayload.Decoded decoded =
                ThermalSequentialFabricatorPacketPayload.decode(
                        "retropolymorph:thermal_selection:v1:minecraft:legacy path");

        assertTrue(decoded.isRecognized());
        assertEquals(new ResourceLocation("minecraft", "legacy path"), decoded.getRecipeId());
    }

    @Test
    void consumesRecognizedFrame() throws IOException {
        DataInputStream input = streamWithUtf(
                ThermalSequentialFabricatorPacketPayload.encode(
                        new ResourceLocation("minecraft", "stick")));

        ThermalSequentialFabricatorPacketPayload.Decoded decoded =
                ThermalSequentialFabricatorPacketPayload.readOptional(input);

        assertTrue(decoded.isRecognized());
        assertEquals(new ResourceLocation("minecraft", "stick"), decoded.getRecipeId());
        assertEquals(0, input.available());
    }

    @Test
    void rewindsForeignUtfExtension() throws IOException {
        DataInputStream input = streamWithUtf("othermod:packet_extension");
        int before = input.available();

        ThermalSequentialFabricatorPacketPayload.Decoded decoded =
                ThermalSequentialFabricatorPacketPayload.readOptional(input);

        assertFalse(decoded.isRecognized());
        assertEquals(before, input.available());
        assertEquals("othermod:packet_extension", input.readUTF());
    }

    @Test
    void rewindsMalformedTrailingBytes() throws IOException {
        byte[] malformed = new byte[] {0, 10, 'x'};
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(malformed));
        int before = input.available();

        ThermalSequentialFabricatorPacketPayload.Decoded decoded =
                ThermalSequentialFabricatorPacketPayload.readOptional(input);

        assertFalse(decoded.isRecognized());
        assertEquals(before, input.available());
    }

    private static DataInputStream streamWithUtf(String value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeUTF(value);
        output.flush();
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }
}
