package com.workshopcraft.simplevoid.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record HazeSpec(int color, float density, int fadeTicks) {
    private static final Codec<Integer> COLOR = Codec.STRING.comapFlatMap(HazeSpec::parseColor, HazeSpec::formatColor);
    public static final Codec<HazeSpec> CODEC = RecordCodecBuilder.<HazeSpec>create(instance -> instance.group(
            COLOR.fieldOf("color").forGetter(HazeSpec::color),
            Codec.FLOAT.optionalFieldOf("density", 0.35F).forGetter(HazeSpec::density),
            Codec.INT.optionalFieldOf("fade_ticks", 20).forGetter(HazeSpec::fadeTicks)
    ).apply(instance, HazeSpec::new));

    public HazeSpec {
        if (density < 0.0F || density > 1.0F) throw new IllegalArgumentException("Haze density must be between 0 and 1");
        if (fadeTicks < 0) throw new IllegalArgumentException("Haze fade_ticks must not be negative");
    }

    public static DataResult<Integer> parseColor(String value) {
        try {
            return DataResult.success(decodeColor(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> exception.getMessage());
        }
    }

    public static int decodeColor(String value) {
        if (value == null) throw new IllegalArgumentException("Haze color is required");
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() == 6) hex = "FF" + hex;
        if (!hex.matches("[0-9a-fA-F]{8}")) throw new IllegalArgumentException("Haze color must be #AARRGGBB or #RRGGBB");
        return (int) Long.parseLong(hex, 16);
    }

    public static String formatColor(int color) {
        return String.format("#%08X", color);
    }
}
