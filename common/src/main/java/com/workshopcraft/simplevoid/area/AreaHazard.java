package com.workshopcraft.simplevoid.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplevoid.SimpleVoid;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record AreaHazard(Identifier type, int airPerTick, int freezePerTick, float damage, int intervalTicks,
                         Identifier damageType, Optional<HazeSpec> haze) {
    public static final Identifier NO_AIR = SimpleVoid.id("no_air");
    public static final Identifier COLD = SimpleVoid.id("cold");
    public static final Identifier DAMAGE = SimpleVoid.id("damage");
    public static final Set<Identifier> TYPES = Set.of(NO_AIR, COLD, DAMAGE);
    public static final HazeSpec DEFAULT_NO_AIR_HAZE = new HazeSpec(HazeSpec.decodeColor("#80A0A0A0"), 0.35F, 20);
    public static final HazeSpec DEFAULT_COLD_HAZE = new HazeSpec(HazeSpec.decodeColor("#80A0C8E6"), 0.35F, 20);
    public static final HazeSpec DEFAULT_DAMAGE_HAZE = new HazeSpec(HazeSpec.decodeColor("#80E65050"), 0.35F, 20);

    public static final Codec<AreaHazard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("type").forGetter(AreaHazard::type),
            Codec.INT.optionalFieldOf("air_per_tick").forGetter(hazard -> Optional.of(hazard.airPerTick())),
            Codec.INT.optionalFieldOf("freeze_per_tick").forGetter(hazard -> Optional.of(hazard.freezePerTick())),
            Codec.FLOAT.optionalFieldOf("damage").forGetter(hazard -> Optional.of(hazard.damage())),
            Codec.INT.optionalFieldOf("interval_ticks").forGetter(hazard -> Optional.of(hazard.intervalTicks())),
            Identifier.CODEC.optionalFieldOf("damage_type").forGetter(hazard -> Optional.of(hazard.damageType())),
            HazeSpec.CODEC.optionalFieldOf("haze").forGetter(AreaHazard::haze)
    ).apply(instance, AreaHazard::create));

    public AreaHazard {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unknown hazard type: " + type);
        if (airPerTick < 0 || freezePerTick < 0 || damage < 0) throw new IllegalArgumentException("Hazard rates must not be negative");
        if (intervalTicks < 1) throw new IllegalArgumentException("Hazard interval_ticks must be at least 1");
        if (damageType.getNamespace().isEmpty() || damageType.getPath().isEmpty()) {
            throw new IllegalArgumentException("Hazard damage_type is invalid");
        }
        haze = haze.map(value -> value);
    }

    public static AreaHazard create(Identifier type, Optional<Integer> air, Optional<Integer> freeze, Optional<Float> damage,
                                    Optional<Integer> interval, Optional<Identifier> damageType, Optional<HazeSpec> haze) {
        boolean noAir = type.equals(NO_AIR);
        boolean cold = type.equals(COLD);
        int airPerTick = air.orElse(noAir ? 1 : 0);
        int freezePerTick = freeze.orElse(cold ? 3 : 0);
        float amount = damage.orElse(noAir ? 2.0F : 1.0F);
        int intervalTicks = interval.orElse(20);
        Identifier resolvedType = damageType.orElse(noAir ? NO_AIR : cold ? COLD : DAMAGE);
        return new AreaHazard(type, airPerTick, freezePerTick, amount, intervalTicks, resolvedType, haze);
    }

    public HazeSpec hazeOrDefault() {
        return haze.orElseGet(() -> type.equals(NO_AIR) ? DEFAULT_NO_AIR_HAZE : type.equals(COLD) ? DEFAULT_COLD_HAZE : DEFAULT_DAMAGE_HAZE);
    }

    public static AreaHazard harsher(AreaHazard first, AreaHazard second) {
        HazeSpec one = first.hazeOrDefault();
        HazeSpec two = second.hazeOrDefault();
        Optional<HazeSpec> haze = two.density() > one.density() ? Optional.of(two) : first.haze().isPresent() || second.haze().isPresent()
                ? Optional.of(one) : Optional.empty();
        return new AreaHazard(first.type(),
                Math.max(first.airPerTick(), second.airPerTick()),
                Math.max(first.freezePerTick(), second.freezePerTick()),
                Math.max(first.damage(), second.damage()),
                Math.min(first.intervalTicks(), second.intervalTicks()),
                first.damage() >= second.damage() ? first.damageType() : second.damageType(),
                haze);
    }

    public String describe() {
        StringBuilder text = new StringBuilder(type.toString());
        if (airPerTick > 0) text.append(" air_per_tick=").append(airPerTick);
        if (freezePerTick > 0) text.append(" freeze_per_tick=").append(freezePerTick);
        if (damage > 0) text.append(" damage=").append(damage).append(" interval_ticks=").append(intervalTicks)
                .append(" damage_type=").append(damageType);
        haze.ifPresent(value -> text.append(" haze=").append(HazeSpec.formatColor(value.color()))
                .append("/").append(value.density()).append("/").append(value.fadeTicks()));
        return text.toString();
    }
}
