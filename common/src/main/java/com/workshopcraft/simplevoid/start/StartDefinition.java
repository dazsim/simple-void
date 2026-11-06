package com.workshopcraft.simplevoid.start;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record StartDefinition(Identifier structure, BlockPos origin, BlockPos spawnOffset, float yaw) {
    public static final Codec<StartDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("structure").forGetter(StartDefinition::structure),
            BlockPos.CODEC.optionalFieldOf("origin", new BlockPos(0, 64, 0)).forGetter(StartDefinition::origin),
            BlockPos.CODEC.fieldOf("spawn_offset").forGetter(StartDefinition::spawnOffset),
            Codec.floatRange(-180.0F, 180.0F).optionalFieldOf("yaw", 0.0F).forGetter(StartDefinition::yaw)
    ).apply(instance, StartDefinition::new));

    public BlockPos spawn() {
        return origin.offset(spawnOffset);
    }
}
