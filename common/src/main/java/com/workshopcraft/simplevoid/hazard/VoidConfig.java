package com.workshopcraft.simplevoid.hazard;

import com.workshopcraft.simplevoid.SimpleVoid;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public final class VoidConfig {
    private static boolean hazards = true;
    private static boolean haze = true;

    private VoidConfig() {}

    public static boolean hazardsEnabled() {
        return hazards;
    }

    public static boolean hazeEnabled() {
        return haze;
    }

    public static void reload() {
        hazards = true;
        haze = true;
        var file = SimpleVoid.configFile();
        if (!Files.exists(file)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Simple Void could not read " + file, exception);
        }
        hazards = flag(properties, "hazards", true);
        haze = flag(properties, "haze", true);
    }

    private static boolean flag(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalStateException("Simple Void config " + key + " must be true or false, not " + value);
    }
}
