package com.workshopcraft.simplevoid.api;

import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.workshopcore.backdrop.Backdrops;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/** Thin wrapper around Workshop Core backdrops. Simple Void does not render a sky of its own. */
public final class VoidBackdrops {
    private VoidBackdrops() {}

    public static void set(ServerLevel level, Identifier id) {
        Backdrops.set(level, id);
    }

    public static void set(ServerLevel level, Identifier id, int ticks) {
        Backdrops.set(level, id, ticks);
    }

    public static void clear(ServerLevel level) {
        Backdrops.clear(level);
    }

    public static void clear(ServerLevel level, int ticks) {
        Backdrops.clear(level, ticks);
    }

    public static Optional<Identifier> get(ServerLevel level) {
        return Backdrops.get(level);
    }

    public static boolean exists(Identifier id) {
        return Backdrops.exists(id);
    }

    public static Set<Identifier> ids() {
        return Backdrops.ids();
    }

    public static void applySetupDefault(ServerLevel level, WorldSetup setup) {
        setup.backdrop().ifPresent(id -> set(level, id));
    }
}
