package com.workshopcraft.simplevoid.setup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreHolder;

public record PlacementCondition(boolean enabled, Optional<String> modLoaded, Optional<Score> score) {
    public static final PlacementCondition ALWAYS = new PlacementCondition(true, Optional.empty(), Optional.empty());
    public static final Codec<PlacementCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(PlacementCondition::enabled),
            Codec.STRING.optionalFieldOf("mod_loaded").forGetter(PlacementCondition::modLoaded),
            Score.CODEC.optionalFieldOf("score").forGetter(PlacementCondition::score)
    ).apply(instance, PlacementCondition::new));

    public boolean test(Predicate<String> loaded, Scoreboard scoreboard) {
        return enabled && modLoaded.map(loaded::test).orElse(true) && score.map(value -> value.test(scoreboard)).orElse(true);
    }

    public record Score(String objective, String holder, int min, int max) {
        public static final Codec<Score> CODEC = RecordCodecBuilder.<Score>create(instance -> instance.group(
                Codec.STRING.fieldOf("objective").forGetter(Score::objective),
                Codec.STRING.fieldOf("holder").forGetter(Score::holder),
                Codec.INT.optionalFieldOf("min", Integer.MIN_VALUE).forGetter(Score::min),
                Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(Score::max)
        ).apply(instance, Score::new)).validate(score -> score.min <= score.max
                ? DataResult.success(score) : DataResult.error(() -> "Score min must not exceed max"));

        private boolean test(Scoreboard scoreboard) {
            var target = scoreboard.getObjective(objective);
            if (target == null) return false;
            var value = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), target);
            return value != null && value.value() >= min && value.value() <= max;
        }
    }
}
