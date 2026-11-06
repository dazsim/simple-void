package com.workshopcraft.simplevoid.start;

import com.workshopcraft.simplevoid.SimpleVoid;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class StartState extends SavedData {
    public static final Codec<StartState> CODEC = StartDefinition.CODEC.optionalFieldOf("placed_start")
            .codec().xmap(StartState::new, state -> state.placed);
    public static final SavedDataType<StartState> TYPE = new SavedDataType<>(
            SimpleVoid.id("start"), () -> new StartState(Optional.empty()), CODEC, DataFixTypes.LEVEL);
    private Optional<StartDefinition> placed;

    private StartState(Optional<StartDefinition> placed) {
        this.placed = placed;
    }

    public static StartState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<StartDefinition> placed() {
        return placed;
    }

    public void complete(StartDefinition definition) {
        placed = Optional.of(definition);
        setDirty();
    }
}
