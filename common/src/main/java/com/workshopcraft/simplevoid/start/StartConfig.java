package com.workshopcraft.simplevoid.start;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.workshopcraft.simplevoid.SimpleVoid;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class StartConfig {
    private StartConfig() {}

    public static StartDefinition load(ResourceManager resources) {
        Path file = SimpleVoid.configFile();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "# Blank uses the data pack selector. Example: start=simplevoid:ship\nstart=\n", StandardCharsets.UTF_8);
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String selection = properties.getProperty("start", "").trim();
            if (selection.isEmpty()) {
                Identifier selector = SimpleVoid.id("simplevoid/start.json");
                try (Reader reader = required(resources, selector).openAsReader()) {
                    selection = JsonParser.parseReader(reader).getAsJsonObject().get("start").getAsString();
                }
            }
            Identifier start = Identifier.tryParse(selection);
            if (start == null) throw new IllegalArgumentException("Invalid start identifier: " + selection);
            Identifier definition = Identifier.fromNamespaceAndPath(start.getNamespace(), "simplevoid/start/" + start.getPath() + ".json");
            try (Reader reader = required(resources, definition).openAsReader()) {
                return StartDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Simple Void could not read the start selection. Check " + file + " and data/<namespace>/simplevoid/start/", exception);
        }
    }

    private static Resource required(ResourceManager resources, Identifier id) {
        return resources.getResource(id).orElseThrow(() -> new IllegalArgumentException("Missing data pack resource: " + id));
    }
}
