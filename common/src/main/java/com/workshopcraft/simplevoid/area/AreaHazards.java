package com.workshopcraft.simplevoid.area;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AreaHazards(Map<String, List<AreaHazard>> byState) {
    public static final AreaHazards NONE = new AreaHazards(Map.of());
    public static final Codec<AreaHazards> CODEC = Codec.unboundedMap(Codec.STRING, AreaHazard.CODEC.listOf()).xmap(AreaHazards::new, AreaHazards::byState);

    public AreaHazards {
        if (byState.size() > 64) throw new IllegalArgumentException("At most 64 hazard states per area");
        var copy = new LinkedHashMap<String, List<AreaHazard>>();
        byState.forEach((state, list) -> {
            AreaDefinition.requireState(state);
            if (list.size() > 8) throw new IllegalArgumentException("At most 8 hazards per area state");
            copy.put(state, List.copyOf(list));
        });
        byState = Map.copyOf(copy);
    }

    public List<AreaHazard> forState(String state) {
        return byState.getOrDefault(state, List.of());
    }

    public boolean defined() {
        return byState.values().stream().anyMatch(list -> !list.isEmpty());
    }
}
