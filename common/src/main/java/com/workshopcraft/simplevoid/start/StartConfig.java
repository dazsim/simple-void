package com.workshopcraft.simplevoid.start;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.setup.WorldSetup;
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

    public static Identifier selected(ResourceManager resources) {
        Path file = SimpleVoid.configFile();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "# Blank uses the data pack selector. Example: setup=simplevoid:ship\nsetup=\n", StandardCharsets.UTF_8);
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String selection = properties.getProperty("setup", "").trim();
            if (selection.isEmpty()) {
                try (Reader reader = required(resources, SimpleVoid.id("simplevoid/world_setup.json")).openAsReader()) {
                    selection = JsonParser.parseReader(reader).getAsJsonObject().get("setup").getAsString();
                }
            }
            Identifier setup = Identifier.tryParse(selection);
            if (setup == null) throw new IllegalArgumentException("Invalid setup identifier: " + selection);
            return setup;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Simple Void could not read the setup selection from " + file, exception);
        }
    }

    public static WorldSetup load(ResourceManager resources, Identifier setup) {
        Path root = SimpleVoid.configFile().getParent().resolve("simplevoid/world_setup").toAbsolutePath().normalize();
        Path override = root.resolve(setup.getNamespace()).resolve(setup.getPath() + ".json").normalize();
        if (!override.startsWith(root)) throw new IllegalArgumentException("Setup config path is outside its directory");
        Identifier definition = Identifier.fromNamespaceAndPath(setup.getNamespace(), "simplevoid/world_setup/" + setup.getPath() + ".json");
        try (Reader reader = Files.exists(override) ? Files.newBufferedReader(override, StandardCharsets.UTF_8)
                : required(resources, definition).openAsReader()) {
            return WorldSetup.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Simple Void could not read setup " + setup + ": " + exception.getMessage(), exception);
        }
    }

    private static Resource required(ResourceManager resources, Identifier id) {
        return resources.getResource(id).orElseThrow(() -> new IllegalArgumentException("Missing data pack resource: " + id));
    }
}
