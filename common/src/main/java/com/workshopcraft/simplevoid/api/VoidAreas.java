package com.workshopcraft.simplevoid.api;

import com.workshopcraft.simplevoid.area.AreaEngine;
import com.workshopcraft.simplevoid.area.AreaCommandHooks;
import com.workshopcraft.simplevoid.area.AreaSavedData;
import com.workshopcraft.simplevoid.area.AreaTransitions;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class VoidAreas {
    private VoidAreas() {}

    public static List<VoidArea> listAreas(ServerLevel level) { return data(level).list(); }
    public static VoidArea getArea(ServerLevel level, String name) { return data(level).require(name); }
    public static VoidArea getArea(ServerLevel level, Identifier id) { return data(level).require(id); }
    public static String getState(ServerLevel level, String name) { return getArea(level, name).state(); }
    public static String getState(ServerLevel level, Identifier id) { return getArea(level, id).state(); }
    public static boolean setState(ServerLevel level, String name, String state) {
        AreaSavedData data = data(level);
        return set(level, data, data.require(name).id(), state);
    }
    public static boolean setState(ServerLevel level, Identifier id, String state) { return set(level, data(level), id, state); }

    private static AreaSavedData data(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Access areas on the server thread");
        return AreaEngine.initialize(level);
    }

    private static boolean set(ServerLevel level, AreaSavedData data, Identifier id, String state) {
        return AreaTransitions.set(data, id, state,
                change -> VoidAreaEvents.post(new VoidAreaEvents.StateChange(level, change.before(), change.after())),
                (area, command) -> AreaCommandHooks.run(level, area, command));
    }
}
