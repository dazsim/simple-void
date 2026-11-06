package com.workshopcraft.simplevoid;

import java.nio.file.Path;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

public final class SimpleVoid {
    public static final String MOD_ID = "simplevoid";
    private static Path configDirectory;
    private static Predicate<String> loaded = id -> false;

    private SimpleVoid() {}

    public static void initialize(Path directory) {
        configDirectory = directory;
    }

    public static void initialize(Path directory, Predicate<String> modLoaded) {
        initialize(directory);
        loaded = modLoaded;
    }

    public static boolean isModLoaded(String id) { return loaded.test(id); }

    public static Path configFile() {
        if (configDirectory == null) throw new IllegalStateException("Simple Void has not initialized");
        return configDirectory.resolve("simplevoid.properties");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
