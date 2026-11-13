package com.workshopcraft.simplevoid.backdrop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record BackdropHooks(boolean enabled, Map<String, List<String>> commands) {
    public static final BackdropHooks NONE = new BackdropHooks(false, Map.of());
    public static final String VANILLA = "vanilla";
    public static final Codec<BackdropHooks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(BackdropHooks::enabled),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("commands", Map.of()).forGetter(BackdropHooks::commands)
    ).apply(instance, BackdropHooks::new));

    public BackdropHooks {
        if (commands.size() > 64) throw new IllegalArgumentException("At most 64 hook backdrops per setup");
        var copy = new LinkedHashMap<String, List<String>>();
        commands.forEach((key, list) -> {
            requireKey(key);
            if (list.size() > 32) throw new IllegalArgumentException("At most 32 commands per backdrop hook");
            for (String command : list) {
                if (command.isBlank() || command.length() > 32767 || command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0) {
                    throw new IllegalArgumentException("Backdrop hook commands must be nonempty single lines up to 32767 characters");
                }
            }
            copy.put(key, List.copyOf(list));
        });
        commands = Map.copyOf(copy);
    }

    public static String requireKey(String key) {
        if (VANILLA.equals(key)) return key;
        Identifier id = Identifier.tryParse(key);
        if (id == null || !key.contains(":")) {
            throw new IllegalArgumentException("Backdrop hook key must be vanilla or a namespaced id: " + key);
        }
        return key;
    }

    public List<String> forBackdrop(Optional<Identifier> id) {
        return enabled ? commands.getOrDefault(key(id), List.of()) : List.of();
    }

    public static String key(Optional<Identifier> id) {
        return id.map(Identifier::toString).orElse(VANILLA);
    }

    public static String expand(String command, Optional<Identifier> current, Optional<Identifier> previous, Identifier dimension) {
        return command.replace("{backdrop}", key(current)).replace("{previous}", key(previous))
                .replace("{dimension}", dimension.toString());
    }
}
