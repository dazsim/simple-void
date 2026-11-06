package com.workshopcraft.simplevoid.validation;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

final class PortalChecks {
    private static int checks;

    static int run(Path resources) throws Exception {
        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK, NbtIo.readCompressed(
                resources.resolve("data/simplevoid/structure/end_portal_island.nbt"), NbtAccounter.unlimitedHeap()));
        check(template.getSize().equals(new BlockPos(9, 5, 9)), "Portal template dimensions");
        check(template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.END_PORTAL).isEmpty(), "No preactivated portal blocks");
        var pattern = EndPortalFrameBlock.getOrCreatePortalShape();
        BlockPos origin = new BlockPos(100, 70, -100);
        for (Rotation rotation : Rotation.values()) {
            var settings = new StructurePlaceSettings().setRotation(rotation);
            var frames = template.filterBlocks(origin, settings, Blocks.END_PORTAL_FRAME);
            var support = new HashSet<BlockPos>();
            template.filterBlocks(origin, settings, Blocks.END_STONE).forEach(block -> support.add(block.pos()));
            BlockPos center = StructureTemplate.calculateRelativePosition(settings, new BlockPos(4, 2, 4)).offset(origin);
            check(frames.size() == 12, "Twelve frames after " + rotation);
            check(frames.stream().noneMatch(block -> block.state().getValue(EndPortalFrameBlock.HAS_EYE)), "Zero eyes after " + rotation);
            check(frames.stream().allMatch(block -> block.pos().relative(block.state().getValue(EndPortalFrameBlock.FACING)).distManhattan(center)
                    < block.pos().distManhattan(center)), "Frames face inward after " + rotation);
            check(frames.stream().allMatch(block -> support.contains(block.pos().below())), "Frames supported after " + rotation);
            Map<BlockPos, BlockState> states = new HashMap<>();
            frames.forEach(block -> states.put(block.pos(), block.state()));
            BlockPos corner = new BlockPos(frames.stream().mapToInt(block -> block.pos().getX()).max().orElseThrow(),
                    origin.getY() + 2, frames.stream().mapToInt(block -> block.pos().getZ()).max().orElseThrow());
            check(pattern.matches(view(states), corner, Direction.DOWN, Direction.SOUTH) == null, "Empty frames do not activate after " + rotation);
            states.replaceAll((pos, state) -> state.setValue(EndPortalFrameBlock.HAS_EYE, true));
            check(pattern.matches(view(states), corner, Direction.DOWN, Direction.SOUTH) != null,
                    "Vanilla portal pattern matches when players fill all eyes after " + rotation);
        }
        System.out.println("Passed " + checks + " plain portal template checks.");
        return checks;
    }

    private static LevelReader view(Map<BlockPos, BlockState> states) {
        return (LevelReader) Proxy.newProxyInstance(LevelReader.class.getClassLoader(), new Class<?>[]{LevelReader.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasChunkAt" -> true;
                    case "getBlockState" -> states.getOrDefault((BlockPos) args[0], Blocks.AIR.defaultBlockState());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
