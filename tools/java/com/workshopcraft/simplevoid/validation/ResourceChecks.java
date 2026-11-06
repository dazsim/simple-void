package com.workshopcraft.simplevoid.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.start.StartConfig;
import com.workshopcraft.simplevoid.start.StartDefinition;
import com.workshopcraft.simplevoid.start.StartState;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;

public final class ResourceChecks {
    private static int checks;

    public static void main(String[] args) {
        try {
            runChecks(args);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void runChecks(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path resources = Path.of(args[0]).resolve("common/src/main/resources");
        Path temporary = Files.createTempDirectory(Path.of("."), "checks-");
        SimpleVoid.initialize(temporary.resolve("config"));
        HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
        Biome biome = Biome.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, vanilla),
                json(resources.resolve("data/simplevoid/worldgen/biome/void.json"))).getOrThrow();
        check(biome.getGenerationSettings().features().isEmpty(), "Biome has no features");
        MappedRegistry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        vanilla.lookupOrThrow(Registries.BIOME).listElements().forEach(holder -> Registry.register(biomes, holder.key(), holder.value()));
        Registry.register(biomes, SimpleVoid.id("void"), biome);
        biomes.freeze();
        HolderLookup.Provider lookup = HolderLookup.Provider.create(Stream.concat(
                vanilla.listRegistries().filter(registry -> !registry.key().equals(Registries.BIOME)), Stream.of(biomes)));
        WorldPreset preset = WorldPreset.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, lookup),
                json(resources.resolve("data/simplevoid/worldgen/world_preset/void.json"))).getOrThrow();
        check(preset.overworld().orElseThrow().generator() instanceof FlatLevelSource, "Flat void generator");
        FlatLevelSource flat = (FlatLevelSource) preset.overworld().orElseThrow().generator();
        check(flat.settings().getLayers().isEmpty(), "No terrain layers");
        check(flat.settings().structureOverrides().orElseThrow().size() == 0, "No generated structures");
        check(flat.settings().getBiome().is(SimpleVoid.id("void")), "Void marker biome");
        StartDefinition island;
        try (MultiPackResourceManager manager = manager(resources)) {
            island = StartConfig.load(manager);
            check(island.structure().equals(SimpleVoid.id("island")), "Island default");
            check(Files.readString(SimpleVoid.configFile()).contains("start="), "Config generated");
            Files.writeString(SimpleVoid.configFile(), "start=simplevoid:ship\n");
            StartDefinition ship = StartConfig.load(manager);
            check(ship.structure().equals(SimpleVoid.id("ship")), "Config selects ship");
            check(ship.spawn().equals(new BlockPos(4, 65, 5)), "Relative ship spawn");
            Files.writeString(SimpleVoid.configFile(), "start=missing:unknown\n");
            expectFailure(() -> StartConfig.load(manager), "Missing definition rejected");
            Files.writeString(SimpleVoid.configFile(), "start=Invalid Identifier!\n");
            expectFailure(() -> StartConfig.load(manager), "Invalid identifier rejected");
            Files.writeString(SimpleVoid.configFile(), "start=\n");
        }
        Path overlay = temporary.resolve("pack");
        Path selector = overlay.resolve("data/simplevoid/simplevoid/start.json");
        Files.createDirectories(selector.getParent());
        Files.writeString(selector, "{\"start\":\"simplevoid:ship\"}");
        try (MultiPackResourceManager manager = manager(resources, overlay)) {
            check(StartConfig.load(manager).structure().equals(SimpleVoid.id("ship")), "Data pack selects ship");
            Files.writeString(SimpleVoid.configFile(), "start=simplevoid:island\n");
            check(StartConfig.load(manager).structure().equals(SimpleVoid.id("island")), "Config overrides data pack");
        }
        Path custom = overlay.resolve("data/homebound/simplevoid/start/ship.json");
        Files.createDirectories(custom.getParent());
        Files.writeString(custom, "{\"structure\":\"homebound:colony_ship\",\"spawn_offset\":[2,1,3]}");
        Files.writeString(selector, "{\"start\":\"homebound:ship\"}");
        Files.writeString(SimpleVoid.configFile(), "start=\n");
        try (MultiPackResourceManager manager = manager(resources, overlay)) {
            StartDefinition homebound = StartConfig.load(manager);
            check(homebound.structure().toString().equals("homebound:colony_ship"), "External ship structure ID");
            check(homebound.origin().equals(new BlockPos(0, 64, 0)), "Default origin");
            Files.writeString(custom, "{\"structure\":\"homebound:colony_ship\",\"spawn_offset\":[2,1,3],\"yaw\":999}");
            expectFailure(() -> StartConfig.load(manager), "Invalid yaw rejected");
        }
        for (String name : List.of("island", "ship")) {
            var tag = NbtIo.readCompressed(resources.resolve("data/simplevoid/structure/" + name + ".nbt"), NbtAccounter.unlimitedHeap());
            StructureTemplate template = new StructureTemplate();
            template.load(BuiltInRegistries.BLOCK, tag);
            StartDefinition definition = StartDefinition.CODEC.parse(JsonOps.INSTANCE,
                    json(resources.resolve("data/simplevoid/simplevoid/start/" + name + ".json"))).getOrThrow();
            var air = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.AIR);
            check(air.stream().anyMatch(block -> block.pos().equals(definition.spawnOffset())), name + " spawn is air in loaded template");
            check(air.stream().anyMatch(block -> block.pos().equals(definition.spawnOffset().above())), name + " headroom in loaded template");
            check(template.getSize().getY() > definition.spawnOffset().getY() + 1, name + " template dimensions");
        }
        StartState state = StartState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        check(state.placed().isEmpty(), "Fresh saved data has no placement");
        state.complete(island);
        JsonElement encoded = StartState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        StartState restored = StartState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        check(restored.placed().orElseThrow().equals(island), "Placement marker codec round trip");
        Path savedData = temporary.resolve("data");
        try (SavedDataStorage storage = new SavedDataStorage(savedData, DataFixers.getDataFixer(), lookup)) {
            storage.set(StartState.TYPE, state);
            storage.saveAndJoin();
        }
        try (SavedDataStorage storage = new SavedDataStorage(savedData, DataFixers.getDataFixer(), lookup)) {
            check(storage.computeIfAbsent(StartState.TYPE).placed().orElseThrow().equals(island), "Placement marker survives disk reload");
        }
        check(Modifier.isStatic(MinecraftServer.class.getDeclaredMethod("setInitialSpawn", ServerLevel.class,
                ServerLevelData.class, boolean.class, boolean.class, LevelLoadListener.class).getModifiers()), "Initial spawn mixin target");
        check(Modifier.isStatic(PlayerSpawnFinder.class.getDeclaredMethod("findSpawn", ServerLevel.class, BlockPos.class).getModifiers()), "Player spawn mixin target");
        System.out.println("Passed " + checks + " headless resource and configuration checks. No server or client started.");
    }

    private static JsonElement json(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static MultiPackResourceManager manager(Path... roots) {
        return new MultiPackResourceManager(PackType.SERVER_DATA, Stream.of(roots).<PackResources>map(path -> new PathPackResources(
                new PackLocationInfo(path.toString(), Component.literal("checks"), PackSource.DEFAULT, Optional.empty()), path)).toList());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            checks++;
            return;
        }
        throw new AssertionError(message);
    }
}
