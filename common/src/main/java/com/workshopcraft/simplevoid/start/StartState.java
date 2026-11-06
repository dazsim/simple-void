package com.workshopcraft.simplevoid.start;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplevoid.SimpleVoid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class StartState extends SavedData {
    public static final Codec<StartState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("selected_setup").forGetter(state -> state.selectedSetup),
            SpawnPoint.CODEC.optionalFieldOf("spawn").forGetter(state -> state.spawn),
            Codec.unboundedMap(Identifier.CODEC, Placed.CODEC).optionalFieldOf("placed", Map.of()).forGetter(state -> state.placed)
    ).apply(instance, StartState::new));
    public static final SavedDataType<StartState> TYPE = new SavedDataType<>(SimpleVoid.id("start"),
            () -> new StartState(Optional.empty(), Optional.empty(), Map.of()), CODEC, DataFixTypes.LEVEL);
    private Optional<Identifier> selectedSetup;
    private Optional<SpawnPoint> spawn;
    private final Map<Identifier, Placed> placed;

    private StartState(Optional<Identifier> setup, Optional<SpawnPoint> spawn, Map<Identifier, Placed> placed) {
        selectedSetup = setup;
        this.spawn = spawn;
        this.placed = new LinkedHashMap<>(placed);
    }

    public static StartState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Identifier> selectedSetup() { return selectedSetup; }
    public Optional<SpawnPoint> spawn() { return spawn; }
    public boolean isPlaced(Identifier key) { return placed.containsKey(key); }
    public Map<Identifier, Placed> placements() { return Map.copyOf(placed); }

    public void select(Identifier setup, SpawnPoint spawn) {
        this.selectedSetup = Optional.of(setup);
        this.spawn = Optional.of(spawn);
        setDirty();
    }

    public void complete(Identifier key, Placed snapshot) {
        if (placed.putIfAbsent(key, snapshot) != null) throw new IllegalStateException("Placement already recorded: " + key);
        setDirty();
    }

    public record SpawnPoint(BlockPos pos, float yaw) {
        public static final Codec<SpawnPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(SpawnPoint::pos),
                Codec.FLOAT.fieldOf("yaw").forGetter(SpawnPoint::yaw)
        ).apply(instance, SpawnPoint::new));
    }

    public record Placed(Identifier structure, BlockPos position, Rotation rotation) {
        public static final Codec<Placed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("structure").forGetter(Placed::structure),
                BlockPos.CODEC.fieldOf("position").forGetter(Placed::position),
                Rotation.CODEC.fieldOf("rotation").forGetter(Placed::rotation)
        ).apply(instance, Placed::new));
    }
}
