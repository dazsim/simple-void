package com.workshopcraft.simplevoid.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplevoid.api.VoidArea;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AreaHooks(boolean enabled, Map<String, List<String>> commands) {
    public static final AreaHooks NONE = new AreaHooks(false, Map.of());
    public static final Codec<AreaHooks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(AreaHooks::enabled),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("commands", Map.of()).forGetter(AreaHooks::commands)
    ).apply(instance, AreaHooks::new));

    public AreaHooks {
        if (commands.size() > 64) throw new IllegalArgumentException("At most 64 hook states per area");
        var copy = new LinkedHashMap<String, List<String>>();
        commands.forEach((state, list) -> {
            AreaDefinition.requireState(state);
            if (list.size() > 32) throw new IllegalArgumentException("At most 32 commands per area state");
            for (String command : list) {
                if (command.isBlank() || command.length() > 32767 || command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0) {
                    throw new IllegalArgumentException("Area hook commands must be nonempty single lines up to 32767 characters");
                }
            }
            copy.put(state, List.copyOf(list));
        });
        commands = Map.copyOf(copy);
    }

    public List<String> forState(String state) {
        return enabled ? commands.getOrDefault(state, List.of()) : List.of();
    }

    public static String expand(String command, VoidArea area, String previous) {
        return command.replace("{area}", area.id().toString()).replace("{state}", area.state()).replace("{previous}", previous)
                .replace("{x}", Integer.toString(area.origin().getX())).replace("{y}", Integer.toString(area.origin().getY()))
                .replace("{z}", Integer.toString(area.origin().getZ())).replace("{rotation}", area.rotation().getSerializedName());
    }
}
