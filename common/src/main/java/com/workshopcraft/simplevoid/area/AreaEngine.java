package com.workshopcraft.simplevoid.area;

import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.api.VoidArea;
import com.workshopcraft.simplevoid.setup.StructurePlacement;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.simplevoid.start.StartConfig;
import com.workshopcraft.simplevoid.start.StartState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class AreaEngine {
    private AreaEngine() {}

    public static List<VoidArea> prepare(Identifier key, StartState.Placed placed, StructurePlacement definition) {
        return definition.areas().stream().map(area -> VoidArea.create(key, placed, area)).toList();
    }

    public static void validate(ServerLevel level, List<VoidArea> areas) {
        for (VoidArea area : areas) {
            if (area.bounds().min().getY() < level.getMinY() || area.bounds().max().getY() >= level.getMaxY()
                    || !level.getWorldBorder().isWithinBounds(area.bounds().min()) || !level.getWorldBorder().isWithinBounds(area.bounds().max())) {
                throw new IllegalArgumentException("Area is outside world bounds: " + area.id());
            }
        }
    }

    public static AreaSavedData initialize(ServerLevel level) {
        AreaSavedData data = AreaSavedData.get(level);
        importExisting(StartState.get(level), data, id -> StartConfig.load(level.getServer().getResourceManager(), id), areas -> validate(level, areas));
        return data;
    }

    public static void importExisting(StartState starts, AreaSavedData data, Function<Identifier, WorldSetup> loader) {
        importExisting(starts, data, loader, areas -> {});
    }

    private static void importExisting(StartState starts, AreaSavedData data, Function<Identifier, WorldSetup> loader, Consumer<List<VoidArea>> validator) {
        Map<Identifier, WorldSetup> setups = new HashMap<>();
        for (var entry : starts.placements().entrySet()) {
            Identifier key = entry.getKey();
            if (data.imported(key)) continue;
            int slash = key.getPath().lastIndexOf('/');
            if (slash < 1) continue;
            Identifier setup = Identifier.fromNamespaceAndPath(key.getNamespace(), key.getPath().substring(0, slash));
            String name = key.getPath().substring(slash + 1);
            try {
                WorldSetup definition = setups.computeIfAbsent(setup, loader);
                var areas = prepare(key, entry.getValue(), definition.placement(name));
                validator.accept(areas);
                data.importPlacement(key, areas);
            } catch (RuntimeException exception) {
                LogUtils.getLogger().warn("Simple Void could not import areas for {}: {}", key, exception.getMessage());
            }
        }
    }
}
