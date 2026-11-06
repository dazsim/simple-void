package com.workshopcraft.simplevoid.setup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record StructurePlacement(String name, Identifier structure, PlacementPosition position, Rotation rotation,
                                 boolean atCreation, PlacementCondition condition) {
    public static final Codec<StructurePlacement> CODEC = RecordCodecBuilder.<StructurePlacement>create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(StructurePlacement::name),
            Identifier.CODEC.fieldOf("structure").forGetter(StructurePlacement::structure),
            PlacementPosition.CODEC.fieldOf("position").forGetter(StructurePlacement::position),
            Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(StructurePlacement::rotation),
            Codec.BOOL.optionalFieldOf("at_creation", true).forGetter(StructurePlacement::atCreation),
            PlacementCondition.CODEC.optionalFieldOf("condition", PlacementCondition.ALWAYS).forGetter(StructurePlacement::condition)
    ).apply(instance, StructurePlacement::new)).validate(placement -> placement.name.matches("[a-z0-9_][a-z0-9_-]*")
            ? DataResult.success(placement) : DataResult.error(() -> "Placement name must be a lowercase name without dots, slashes or spaces"));

    public Identifier key(Identifier setup) {
        return Identifier.fromNamespaceAndPath(setup.getNamespace(), setup.getPath() + "/" + name);
    }
}
