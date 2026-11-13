package com.workshopcraft.simplevoid.hazard;

import com.workshopcraft.simplevoid.api.VoidArea;
import com.workshopcraft.simplevoid.area.AreaHazards;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.simplevoid.start.StartConfig;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class HazardCatalog {
    private static final Map<Identifier, AreaHazards> CACHE = new ConcurrentHashMap<>();

    private HazardCatalog() {}

    public static void invalidate() {
        CACHE.clear();
    }

    public static AreaHazards defined(VoidArea area, Function<Identifier, WorldSetup> setups) {
        return lookup(area, setups);
    }

    public static AreaHazards defined(ServerLevel level, VoidArea area) {
        return CACHE.computeIfAbsent(area.id(), id -> lookup(area, setup -> StartConfig.load(level.getServer().getResourceManager(), setup)));
    }

    public static Map<String, List<com.workshopcraft.simplevoid.area.AreaHazard>> definedByState(VoidArea area, Function<Identifier, WorldSetup> setups) {
        return lookup(area, setups).byState();
    }

    private static AreaHazards lookup(VoidArea area, Function<Identifier, WorldSetup> setups) {
        Identifier key = area.placement();
        int slash = key.getPath().lastIndexOf('/');
        if (slash < 1) return AreaHazards.NONE;
        Identifier setup = Identifier.fromNamespaceAndPath(key.getNamespace(), key.getPath().substring(0, slash));
        String name = key.getPath().substring(slash + 1);
        try {
            var placement = setups.apply(setup).placement(name);
            for (var definition : placement.areas()) {
                if (definition.name().equals(area.name())) return definition.hazards();
            }
        } catch (RuntimeException ignored) {
            return AreaHazards.NONE;
        }
        return AreaHazards.NONE;
    }
}
