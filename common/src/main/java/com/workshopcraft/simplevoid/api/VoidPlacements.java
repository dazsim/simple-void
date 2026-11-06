package com.workshopcraft.simplevoid.api;

import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.simplevoid.start.PlacementEngine;
import com.workshopcraft.simplevoid.start.StartConfig;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class VoidPlacements {
    private VoidPlacements() {}

    public static Result placeSetup(ServerLevel level, Identifier setup) {
        return place(level, setup, null);
    }

    public static Result placePlacement(ServerLevel level, Identifier setup, String name) {
        if (name == null) throw new IllegalArgumentException("Placement name is required");
        return place(level, setup, name);
    }

    private static Result place(ServerLevel level, Identifier id, String name) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Place structures on the server thread");
        WorldSetup setup = StartConfig.load(level.getServer().getResourceManager(), id);
        return PlacementEngine.place(level, id, setup, name, false, level.getRespawnData().pos());
    }

    public record Result(List<String> placed, List<String> alreadyPlaced, List<String> conditionFailed) {
        public Result {
            placed = List.copyOf(placed);
            alreadyPlaced = List.copyOf(alreadyPlaced);
            conditionFailed = List.copyOf(conditionFailed);
        }
    }
}
