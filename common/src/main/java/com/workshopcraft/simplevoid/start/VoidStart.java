package com.workshopcraft.simplevoid.start;

import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.SimpleVoid;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
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
        StartDefinition definition;
        if (state.placed().isPresent()) {
            definition = state.placed().get();
        } else {
            definition = StartConfig.load(level.getServer().getResourceManager());
            StructureTemplate template = level.getStructureManager().get(definition.structure())
                    .orElseThrow(() -> new IllegalStateException("Simple Void start structure is missing: " + definition.structure()));
            validateBounds(level, definition, template.getSize());
            StructurePlaceSettings placement = new StructurePlaceSettings().setIgnoreEntities(false);
            if (!template.placeInWorld(level, definition.origin(), definition.origin(), placement, level.getRandom(), 3)) {
                throw new IllegalStateException("Simple Void could not place start structure: " + definition.structure());
            }
            if (!safeSpawn(level, definition.spawn())) {
                throw new IllegalStateException("Simple Void spawn_offset needs a solid floor and two clear, dry blocks: " + definition.spawn());
            }
            state.complete(definition);
            LogUtils.getLogger().info("Simple Void placed {} at {}; spawn {}", definition.structure(), definition.origin(), definition.spawn());
        }
        levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), definition.spawn(), definition.yaw(), 0.0F));
        level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
        return true;
    }

    public static Optional<Vec3> exactSpawn(ServerLevel level, BlockPos suggestion) {
        if (!isVoid(level) || level.getGameRules().get(GameRules.RESPAWN_RADIUS) != 0) return Optional.empty();
        return StartState.get(level).placed()
                .filter(start -> start.spawn().equals(suggestion))
                .filter(start -> safeSpawn(level, suggestion))
                .map(start -> Vec3.atBottomCenterOf(suggestion));
    }

    private static boolean safeSpawn(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.noCollision(null, EntityType.PLAYER.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos)), true);
    }

    private static void validateBounds(ServerLevel level, StartDefinition definition, Vec3i size) {
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1
                || size.getX() > 256 || size.getY() > 256 || size.getZ() > 256
                || (long) size.getX() * size.getY() * size.getZ() > 1_048_576) {
            throw new IllegalArgumentException("Simple Void start must fit 256 blocks per axis and 1048576 blocks total");
        }
        BlockPos offset = definition.spawnOffset();
        if (offset.getX() < 0 || offset.getY() < 1 || offset.getZ() < 0
                || offset.getX() >= size.getX() || offset.getY() + 1 >= size.getY() || offset.getZ() >= size.getZ()) {
            throw new IllegalArgumentException("Simple Void spawn_offset must be inside the structure, with room for the player");
        }
        BlockPos end = definition.origin().offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        if (definition.origin().getY() < level.getMinY() || end.getY() >= level.getMaxY()
                || !level.getWorldBorder().isWithinBounds(definition.origin()) || !level.getWorldBorder().isWithinBounds(end)) {
            throw new IllegalArgumentException("Simple Void start structure is outside the world bounds");
        }
    }
}
