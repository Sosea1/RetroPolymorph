package com.sosea1.retropolymorph.mixin;

import net.minecraftforge.common.config.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class EarlyIntegrationConfigTest {
    @TempDir Path directory;

    @Test
    void switchesMatchForgeConfigWithoutChangingTheFile() throws Exception {
        File file = directory.resolve("retropolymorph.cfg").toFile();
        java.lang.reflect.Field home = net.minecraftforge.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        Object previousHome = home.get(null);
        Configuration forge;
        try {
            home.set(null, directory.toFile());
            forge = new Configuration(file);
        } finally {
            home.set(null, previousHome);
        }
        forge.get("integrations", "ae2", false);
        forge.get("integrations", "refinedStorage", false);
        forge.get("integrations", "gregTech", true);
        forge.get("selector", "ae2", true);
        forge.save();
        byte[] original = Files.readAllBytes(file.toPath());
        EarlyIntegrationConfig early = EarlyIntegrationConfig.read(file);
        assertFalse(early.isEnabled("ae2"));
        assertFalse(early.isEnabled("refinedStorage"));
        assertTrue(early.isEnabled("gregTech"));
        assertTrue(early.isEnabled("rftools"));
        assertArrayEquals(original, Files.readAllBytes(file.toPath()));
    }

    @Test
    void ignoresCommentsOtherCategoriesAndListContents() throws Exception {
        Path path = directory.resolve("retropolymorph.cfg");
        Files.write(path, ("selector {\nB:ae2=false\n}\n"
                + "policy {\nS:preferredRecipes <\nintegrations {\nB:jei=false\n>\n}\n"
                + "\"integrations\" {\n# B:jei=false\nB:\"ae2\"=false\nB:jei=true\n}\n")
                .getBytes(StandardCharsets.UTF_8));
        EarlyIntegrationConfig early = EarlyIntegrationConfig.read(path.toFile());
        assertFalse(early.isEnabled("ae2"));
        assertTrue(early.isEnabled("jei"));
    }

    @Test
    void missingConfigLeavesDefaultsEnabledWithoutCreatingFile() throws Exception {
        File file = directory.resolve("absent.cfg").toFile();
        assertTrue(EarlyIntegrationConfig.read(file).isEnabled("ae2"));
        assertFalse(file.exists());
    }
}
