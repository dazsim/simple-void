package com.workshopcraft.simplevoid.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.api.VoidArea;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class AreaSavedData extends SavedData {
    public static final Codec<AreaSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, VoidArea.CODEC).optionalFieldOf("areas", Map.of()).forGetter(data -> data.areas),
            Identifier.CODEC.listOf().optionalFieldOf("imported_placements", List.of()).forGetter(data -> data.imported.stream().sorted().toList())
    ).apply(instance, AreaSavedData::new));
    public static final SavedDataType<AreaSavedData> TYPE = new SavedDataType<>(SimpleVoid.id("areas"),
            () -> new AreaSavedData(Map.of(), List.of()), CODEC, DataFixTypes.LEVEL);
    private final Map<Identifier, VoidArea> areas;
    private final Set<Identifier> imported;

    private AreaSavedData(Map<Identifier, VoidArea> areas, List<Identifier> imported) {
        areas.forEach((key, area) -> {
            if (!key.equals(area.id())) throw new IllegalArgumentException("Area key does not match its id: " + key);
        });
        this.areas = new LinkedHashMap<>(areas);
        this.imported = new HashSet<>(imported);
    }

    public static AreaSavedData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public boolean imported(Identifier placement) { return imported.contains(placement); }
    public List<VoidArea> list() { return areas.values().stream().sorted(java.util.Comparator.comparing(area -> area.id().toString())).toList(); }

    public VoidArea require(String name) {
        if (name.contains(":")) return require(Identifier.parse(name));
        var matches = areas.values().stream().filter(area -> area.name().equals(name)).toList();
        if (matches.isEmpty()) throw new IllegalArgumentException("Unknown area: " + name);
        if (matches.size() > 1) throw new IllegalArgumentException("Ambiguous area " + name + "; use one of " + matches.stream().map(area -> area.id().toString()).sorted().toList());
        return matches.getFirst();
    }

    public VoidArea require(Identifier id) {
        VoidArea area = areas.get(id);
        if (area == null) throw new IllegalArgumentException("Unknown area: " + id);
        return area;
    }

    public void importPlacement(Identifier placement, List<VoidArea> additions) {
        if (imported(placement)) return;
        var keys = new HashSet<Identifier>();
        for (VoidArea area : additions) {
            if (!area.placement().equals(placement) || !keys.add(area.id()) || areas.containsKey(area.id())) {
                throw new IllegalArgumentException("Conflicting area: " + area.id());
            }
        }
        additions.forEach(area -> areas.put(area.id(), area));
        imported.add(placement);
        setDirty();
    }

    VoidArea update(Identifier id, String state) {
        VoidArea next = require(id).withState(state);
        areas.put(id, next);
        setDirty();
        return next;
    }
}
