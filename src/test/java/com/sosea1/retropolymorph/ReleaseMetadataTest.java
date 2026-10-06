package com.sosea1.retropolymorph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.common.Mod;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseMetadataTest {
    @Test
    void forgeAnnotationAndMetadataExposeTheSameReleaseIdentity() throws Exception {
        JsonObject metadata = metadata();
        Mod mod = RetroPolymorph.class.getAnnotation(Mod.class);
        assertEquals("1.0.0", mod.version());
        assertEquals(mod.version(), metadata.get("version").getAsString());
        assertEquals(mod.modid(), metadata.get("modid").getAsString());
        assertEquals(mod.name(), metadata.get("name").getAsString());
        assertEquals("com.sosea1.retropolymorph.RetroPolymorph", RetroPolymorph.class.getName());
        assertEquals(1, metadata.getAsJsonArray("authorList").size());
        assertEquals("Sosea1", metadata.getAsJsonArray("authorList").get(0).getAsString());
        assertTrue(metadata.get("credits").getAsString().contains("TheIllusiveC4"));
        assertFalse(metadata.get("credits").getAsString().contains("Sosea1"));
        assertEquals("mixinbooter", metadata.getAsJsonArray("requiredMods").get(0).getAsString());
    }

    @Test
    void modListLogoLoadsFromItsMetadataPathWithoutBundlingTheLargeSource() throws Exception {
        JsonObject metadata = metadata();
        assertEquals("assets/retropolymorph/logo.png", metadata.get("logoFile").getAsString());
        try (InputStream input = RetroPolymorph.class.getResourceAsStream("/" + metadata.get("logoFile").getAsString())) {
            assertNotNull(input);
            BufferedImage image = ImageIO.read(input);
            assertNotNull(image);
            assertEquals(256, image.getWidth());
            assertEquals(256, image.getHeight());
        }
    }

    private JsonObject metadata() throws Exception {
        try (InputStream input = RetroPolymorph.class.getResourceAsStream("/mcmod.info")) {
            assertNotNull(input);
            return new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonArray().get(0).getAsJsonObject();
        }
    }
}
