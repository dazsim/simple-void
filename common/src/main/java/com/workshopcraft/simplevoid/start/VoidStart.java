package com.workshopcraft.simplevoid.start;

import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

public final class VoidStart {
    private VoidStart() {}

    public static boolean isVoid(ServerLevel level) {
        return level.dimension().equals(Level.OVERWORLD)
                && level.getChunkSource().getGenerator() instanceof FlatLevelSource flat
                && flat.settings().getBiome().is(ResourceKey.create(Registries.BIOME, SimpleVoid.id("void")))
                && flat.settings().getLayers().stream().allMatch(state -> state.isAir());
    }

    public static boolean initialize(ServerLevel level, ServerLevelData levelData) {
        if (!isVoid(level)) return false;
        StartState state = StartState.get(level);
        if (state.spawn().isEmpty()) {
            var resources = level.getServer().getResourceManager();
            var id = state.selectedSetup().orElseGet(() -> StartConfig.selected(resources));
            WorldSetup setup = StartConfig.load(resources, id);
            var template = level.getStructureManager().get(setup.spawnPlacement().structure())
                    .orElseThrow(() -> new IllegalStateException("Spawn structure is missing: " + setup.spawnPlacement().structure()));
            BlockPos offset = setup.spawn().offset();
            var size = template.getSize();
            if (offset.getX() < 0 || offset.getY() < 1 || offset.getZ() < 0
                    || offset.getX() >= size.getX() || offset.getY() + 1 >= size.getY() || offset.getZ() >= size.getZ()) {
                throw new IllegalArgumentException("Spawn offset must be inside the structure, including player headroom");
            }
            PlacementEngine.place(level, id, setup, null, true, setup.spawnPosition());
            if (!safeSpawn(level, setup.spawnPosition())) {
                throw new IllegalStateException("Spawn needs a solid floor and two clear, dry blocks: " + setup.spawnPosition());
            }
            state.select(id, new StartState.SpawnPoint(setup.spawnPosition(), setup.spawn().yaw()));
        }
        var spawn = state.spawn().orElseThrow();
        levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), spawn.pos(), spawn.yaw(), 0.0F));
        level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
        return true;
    }

    public static Optional<Vec3> exactSpawn(ServerLevel level, BlockPos suggestion) {
        if (!isVoid(level) || level.getGameRules().get(GameRules.RESPAWN_RADIUS) != 0) return Optional.empty();
        return StartState.get(level).spawn().filter(spawn -> spawn.pos().equals(suggestion))
                .filter(spawn -> safeSpawn(level, suggestion)).map(spawn -> Vec3.atBottomCenterOf(suggestion));
    }

    private static boolean safeSpawn(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.noCollision(null, EntityType.PLAYER.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos)), true);
    }
}
