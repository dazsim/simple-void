package com.workshopcraft.simplevoid.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AreaDefinition(String name, AreaBounds bounds, String initialState, AreaHooks onStateChange) {
    public static final Codec<AreaDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(AreaDefinition::name),
            AreaBounds.CODEC.fieldOf("bounds").forGetter(AreaDefinition::bounds),
            Codec.STRING.optionalFieldOf("initial_state", "damaged").forGetter(AreaDefinition::initialState),
            AreaHooks.CODEC.optionalFieldOf("on_state_change", AreaHooks.NONE).forGetter(AreaDefinition::onStateChange)
    ).apply(instance, AreaDefinition::new));

    public AreaDefinition {
        if (!name.matches("[a-z0-9_][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("Area name needs 1 to 64 lowercase letters, digits, underscores or hyphens");
        requireState(initialState);
        if ((long) bounds.max().getX() - bounds.min().getX() > 4095
                || (long) bounds.max().getY() - bounds.min().getY() > 4095
                || (long) bounds.max().getZ() - bounds.min().getZ() > 4095
                || Math.abs((long) bounds.min().getX()) > 4096 || Math.abs((long) bounds.max().getX()) > 4096
                || Math.abs((long) bounds.min().getY()) > 4096 || Math.abs((long) bounds.max().getY()) > 4096
                || Math.abs((long) bounds.min().getZ()) > 4096 || Math.abs((long) bounds.max().getZ()) > 4096) {
            throw new IllegalArgumentException("Local area bounds must be within +/-4096 with at most 4096 blocks per axis");
        }
    }

    public static String requireState(String state) {
        if (state == null || !state.matches("[a-z0-9_][a-z0-9_.-]{0,63}")) {
            throw new IllegalArgumentException("State needs 1 to 64 lowercase letters, digits, underscores, dots or hyphens");
        }
        return state;
    }
}
