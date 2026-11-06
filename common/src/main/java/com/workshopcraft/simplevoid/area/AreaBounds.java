package com.workshopcraft.simplevoid.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record AreaBounds(BlockPos min, BlockPos max) {
    public static final Codec<AreaBounds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("min").forGetter(AreaBounds::min),
            BlockPos.CODEC.fieldOf("max").forGetter(AreaBounds::max)
    ).apply(instance, AreaBounds::new));

    public AreaBounds {
        min = min.immutable();
        max = max.immutable();
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
            throw new IllegalArgumentException("Area bounds min must not exceed max");
        }
    }

    public AreaBounds transform(BlockPos origin, Rotation rotation) {
        BlockPos a = StructureTemplate.transform(min, Mirror.NONE, rotation, BlockPos.ZERO).offset(origin);
        BlockPos b = StructureTemplate.transform(max, Mirror.NONE, rotation, BlockPos.ZERO).offset(origin);
        return new AreaBounds(new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }
}
