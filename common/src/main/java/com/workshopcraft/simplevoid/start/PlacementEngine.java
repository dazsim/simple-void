package com.workshopcraft.simplevoid.start;

import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.api.VoidPlacements;
import com.workshopcraft.simplevoid.setup.StructurePlacement;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class PlacementEngine {
    private PlacementEngine() {}

    public static VoidPlacements.Result place(ServerLevel level, Identifier id, WorldSetup setup, String name,
                                              boolean creation, BlockPos spawn) {
        StartState state = StartState.get(level);
        List<String> already = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<Plan> plans = new ArrayList<>();
        List<StructurePlacement> candidates = name == null ? setup.placements() : List.of(setup.placement(name));
        for (StructurePlacement placement : candidates) {
            if (creation && !placement.atCreation()) continue;
            if (state.isPlaced(placement.key(id))) {
                already.add(placement.name());
                continue;
            }
            if (!placement.condition().test(SimpleVoid::isModLoaded, level.getServer().getScoreboard())) {
                skipped.add(placement.name());
                continue;
            }
            BlockPos origin = placement.position().resolve(spawn);
            StructureTemplate template = level.getStructureManager().get(placement.structure())
                    .orElseThrow(() -> new IllegalStateException("Missing structure for " + id + "/" + placement.name() + ": " + placement.structure()));
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(placement.rotation()).setIgnoreEntities(false);
            validateBounds(level, template.getSize(), template.getBoundingBox(settings, origin));
            plans.add(new Plan(placement, template, settings, origin));
        }
        if (creation && skipped.contains(setup.spawn().placement())) {
            throw new IllegalStateException("Spawn structure condition did not pass: " + setup.spawn().placement());
        }
        List<String> placed = new ArrayList<>();
        for (Plan plan : plans) {
            if (!plan.template.placeInWorld(level, plan.origin, plan.origin, plan.settings, level.getRandom(), 3)) {
                throw new IllegalStateException("Could not place " + id + "/" + plan.placement.name());
            }
            state.complete(plan.placement.key(id), new StartState.Placed(plan.placement.structure(), plan.origin, plan.placement.rotation()));
            placed.add(plan.placement.name());
            LogUtils.getLogger().info("Simple Void placed {}/{} ({}) at {}", id, plan.placement.name(), plan.placement.structure(), plan.origin);
        }
        return new VoidPlacements.Result(placed, already, skipped);
    }

    private static void validateBounds(ServerLevel level, Vec3i size, BoundingBox box) {
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1
                || size.getX() > 256 || size.getY() > 256 || size.getZ() > 256
                || (long) size.getX() * size.getY() * size.getZ() > 1_048_576) {
            throw new IllegalArgumentException("Structure must fit 256 blocks per axis and 1048576 blocks total");
        }
        if (box.minY() < level.getMinY() || box.maxY() >= level.getMaxY()
                || !level.getWorldBorder().isWithinBounds(new BlockPos(box.minX(), box.minY(), box.minZ()))
                || !level.getWorldBorder().isWithinBounds(new BlockPos(box.maxX(), box.maxY(), box.maxZ()))) {
            throw new IllegalArgumentException("Structure is outside the world bounds");
        }
    }

    private record Plan(StructurePlacement placement, StructureTemplate template, StructurePlaceSettings settings, BlockPos origin) {}
}
