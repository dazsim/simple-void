package com.workshopcraft.simplevoid.setup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public record PlacementPosition(Optional<BlockPos> fixed, Optional<BlockPos> spawnOffset) {
    public static final Codec<PlacementPosition> CODEC = RecordCodecBuilder.<PlacementPosition>create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("fixed").forGetter(PlacementPosition::fixed),
            BlockPos.CODEC.optionalFieldOf("spawn_offset").forGetter(PlacementPosition::spawnOffset)
    ).apply(instance, PlacementPosition::new)).validate(position -> position.fixed.isPresent() != position.spawnOffset.isPresent()
            ? DataResult.success(position) : DataResult.error(() -> "Position needs exactly one of fixed or spawn_offset"));

    public BlockPos resolve(BlockPos spawn) {
        return fixed.orElseGet(() -> spawn.offset(spawnOffset.orElseThrow()));
    }
}
