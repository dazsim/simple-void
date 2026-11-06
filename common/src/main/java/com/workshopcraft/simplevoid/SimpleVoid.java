package com.workshopcraft.simplevoid;

import java.nio.file.Path;
import net.minecraft.resources.Identifier;

public final class SimpleVoid {
    public static final String MOD_ID = "simplevoid";
    private static Path configDirectory;

    private SimpleVoid() {}

    public static void initialize(Path directory) {
        configDirectory = directory;
    }

    public static Path configFile() {
        if (configDirectory == null) throw new IllegalStateException("Simple Void has not initialized");
        return configDirectory.resolve("simplevoid.properties");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
