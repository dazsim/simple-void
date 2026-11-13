package com.workshopcraft.simplevoid.setup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplevoid.backdrop.BackdropHooks;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record WorldSetup(Spawn spawn, List<StructurePlacement> placements, Optional<Identifier> backdrop,
                         BackdropHooks onBackdropChange) {
    public static final Codec<WorldSetup> CODEC = RecordCodecBuilder.<WorldSetup>create(instance -> instance.group(
            Spawn.CODEC.fieldOf("spawn").forGetter(WorldSetup::spawn),
            StructurePlacement.CODEC.listOf().fieldOf("placements").forGetter(WorldSetup::placements),
            Identifier.CODEC.optionalFieldOf("backdrop").forGetter(WorldSetup::backdrop),
            BackdropHooks.CODEC.optionalFieldOf("on_backdrop_change", BackdropHooks.NONE).forGetter(WorldSetup::onBackdropChange)
    ).apply(instance, WorldSetup::new)).validate(WorldSetup::validate);

    public WorldSetup(Spawn spawn, List<StructurePlacement> placements) {
        this(spawn, placements, Optional.empty(), BackdropHooks.NONE);
    }

    private static DataResult<WorldSetup> validate(WorldSetup setup) {
        if (setup.placements.isEmpty() || setup.placements.size() > 128) return DataResult.error(() -> "Setup needs 1 to 128 placements");
        var names = new HashSet<String>();
        for (StructurePlacement placement : setup.placements) {
            if (!names.add(placement.name())) return DataResult.error(() -> "Duplicate placement name: " + placement.name());
        }
        var start = setup.placements.stream().filter(placement -> placement.name().equals(setup.spawn.placement)).findFirst();
        if (start.isEmpty()) return DataResult.error(() -> "Spawn must name one placement");
        if (start.get().position().fixed().isEmpty() || !start.get().atCreation()) {
            return DataResult.error(() -> "Spawn placement must use a fixed position and at_creation=true");
        }
        return DataResult.success(setup);
    }

    public StructurePlacement spawnPlacement() {
        return placement(spawn.placement);
    }

    public StructurePlacement placement(String name) {
        return placements.stream().filter(placement -> placement.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown placement: " + name));
    }

    public BlockPos spawnPosition() {
        StructurePlacement placement = spawnPlacement();
        return StructureTemplate.transform(spawn.offset, Mirror.NONE, placement.rotation(), BlockPos.ZERO)
                .offset(placement.position().fixed().orElseThrow());
    }

    public record Spawn(String placement, BlockPos offset, float yaw) {
        public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("placement").forGetter(Spawn::placement),
                BlockPos.CODEC.fieldOf("offset").forGetter(Spawn::offset),
                Codec.floatRange(-180.0F, 180.0F).optionalFieldOf("yaw", 0.0F).forGetter(Spawn::yaw)
        ).apply(instance, Spawn::new));
    }
}
