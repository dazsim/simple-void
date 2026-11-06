package com.workshopcraft.simplevoid.setup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplevoid.area.AreaDefinition;
import java.util.HashSet;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record StructurePlacement(String name, Identifier structure, PlacementPosition position, Rotation rotation,
                                 boolean atCreation, PlacementCondition condition, List<AreaDefinition> areas) {
    public static final Codec<StructurePlacement> CODEC = RecordCodecBuilder.<StructurePlacement>create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(StructurePlacement::name),
            Identifier.CODEC.fieldOf("structure").forGetter(StructurePlacement::structure),
            PlacementPosition.CODEC.fieldOf("position").forGetter(StructurePlacement::position),
            Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(StructurePlacement::rotation),
            Codec.BOOL.optionalFieldOf("at_creation", true).forGetter(StructurePlacement::atCreation),
            PlacementCondition.CODEC.optionalFieldOf("condition", PlacementCondition.ALWAYS).forGetter(StructurePlacement::condition),
            AreaDefinition.CODEC.listOf().optionalFieldOf("areas", List.of()).forGetter(StructurePlacement::areas)
    ).apply(instance, StructurePlacement::new)).validate(placement -> placement.name.matches("[a-z0-9_][a-z0-9_-]*")
            ? DataResult.success(placement) : DataResult.error(() -> "Placement name must be a lowercase name without dots, slashes or spaces"));

    public StructurePlacement {
        areas = List.copyOf(areas);
        if (areas.size() > 128) throw new IllegalArgumentException("At most 128 areas per placement");
        var names = new HashSet<String>();
        for (var area : areas) {
            if (!names.add(area.name())) throw new IllegalArgumentException("Duplicate area name: " + area.name());
        }
    }

    public Identifier key(Identifier setup) {
        return Identifier.fromNamespaceAndPath(setup.getNamespace(), setup.getPath() + "/" + name);
    }
}
