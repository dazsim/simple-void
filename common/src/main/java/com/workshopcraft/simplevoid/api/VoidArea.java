package com.workshopcraft.simplevoid.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplevoid.area.AreaBounds;
import com.workshopcraft.simplevoid.area.AreaDefinition;
import com.workshopcraft.simplevoid.area.AreaHooks;
import com.workshopcraft.simplevoid.start.StartState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record VoidArea(Identifier id, String name, Identifier placement, BlockPos origin, Rotation rotation,
                       AreaBounds bounds, String state, AreaHooks onStateChange) {
    public static final Codec<VoidArea> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(VoidArea::id),
            Codec.STRING.fieldOf("name").forGetter(VoidArea::name),
            Identifier.CODEC.fieldOf("placement").forGetter(VoidArea::placement),
            BlockPos.CODEC.fieldOf("origin").forGetter(VoidArea::origin),
            Rotation.CODEC.fieldOf("rotation").forGetter(VoidArea::rotation),
            AreaBounds.CODEC.fieldOf("bounds").forGetter(VoidArea::bounds),
            Codec.STRING.fieldOf("state").forGetter(VoidArea::state),
            AreaHooks.CODEC.optionalFieldOf("on_state_change", AreaHooks.NONE).forGetter(VoidArea::onStateChange)
    ).apply(instance, VoidArea::new));

    public VoidArea {
        origin = origin.immutable();
        AreaDefinition.requireState(state);
    }

    public static VoidArea create(Identifier placement, StartState.Placed placed, AreaDefinition definition) {
        Identifier id = Identifier.fromNamespaceAndPath(placement.getNamespace(), placement.getPath() + "/" + definition.name());
        return new VoidArea(id, definition.name(), placement, placed.position(), placed.rotation(),
                definition.bounds().transform(placed.position(), placed.rotation()), definition.initialState(), definition.onStateChange());
    }

    public VoidArea withState(String next) {
        return new VoidArea(id, name, placement, origin, rotation, bounds, next, onStateChange);
    }
}
