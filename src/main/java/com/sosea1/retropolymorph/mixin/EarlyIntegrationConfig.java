package com.sosea1.retropolymorph.mixin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Reads only integration switches before Forge preInit; never creates or saves the config. */
final class EarlyIntegrationConfig {
    private final Map<String, Boolean> switches = new HashMap<>();

    static EarlyIntegrationConfig read(File file) throws IOException {
        EarlyIntegrationConfig config = new EarlyIntegrationConfig();
        if (!file.exists()) { return config; }
        int depth = 0;
        int integrationDepth = -1;
        boolean list = false;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) { continue; }
                if (list) {
                    if (line.equals(">")) { list = false; }
                    continue;
                }
                if (line.endsWith("<")) { list = true; continue; }
                if (line.endsWith("{")) {
                    String category = unquote(line.substring(0, line.length() - 1).trim());
                    depth++;
                    if (depth == 1 && category.equals("integrations")) { integrationDepth = depth; }
                } else if (line.equals("}")) {
                    if (depth == integrationDepth) { integrationDepth = -1; }
                    depth--;
                } else if (depth == integrationDepth && line.startsWith("B:")) {
                    int equals = line.indexOf('=');
                    if (equals < 0) { continue; }
                    String key = unquote(line.substring(2, equals).trim());
                    String value = line.substring(equals + 1).trim();
                    if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("true")) {
                        config.switches.put(key, Boolean.parseBoolean(value));
                    }
                }
            }
        }
        return config;
    }

    boolean isEnabled(String key) { return !Boolean.FALSE.equals(this.switches.get(key)); }

    private static String unquote(String text) {
        return text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2
                ? text.substring(1, text.length() - 1) : text;
    }
}
