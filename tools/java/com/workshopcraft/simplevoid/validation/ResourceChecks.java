package com.workshopcraft.simplevoid.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.start.StartConfig;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.simplevoid.setup.PlacementPosition;
import com.workshopcraft.simplevoid.setup.StructurePlacement;
import com.workshopcraft.simplevoid.setup.PlacementCondition;
import com.workshopcraft.simplevoid.command.VoidCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
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
        WorldSetup skyblock;
        try (MultiPackResourceManager manager = manager(resources)) {
            check(StartConfig.selected(manager).equals(SimpleVoid.id("skyblock")), "Default setup");
            skyblock = StartConfig.load(manager, StartConfig.selected(manager));
            check(skyblock.placements().size() == 3, "Multiple placements including deferred example");
            check(skyblock.spawnPlacement().structure().equals(SimpleVoid.id("island")), "Island is spawn structure");
            check(skyblock.spawnPosition().equals(new BlockPos(4, 67, 2)), "Absolute spawn position");
            check(skyblock.placement("distant_example").position().resolve(skyblock.spawnPosition()).equals(new BlockPos(260, 64, 2)), "Spawn-relative placement");
            check(skyblock.placement("distant_example").rotation() == Rotation.CLOCKWISE_90, "Rotation decoded");
            check(!skyblock.placement("later_example").atCreation(), "Deferred placement decoded");
            check(Files.readString(SimpleVoid.configFile()).contains("setup="), "Config generated");
            Files.writeString(SimpleVoid.configFile(), "setup=simplevoid:ship\n");
            check(StartConfig.selected(manager).equals(SimpleVoid.id("ship")), "Config selects ship");
            var ship = StartConfig.load(manager, StartConfig.selected(manager));
            check(ship.spawnPosition().equals(new BlockPos(4, 65, 5)), "Interior ship spawn");
            expectFailure(() -> StartConfig.load(manager, SimpleVoid.id("missing")), "Missing setup rejected");
            Files.writeString(SimpleVoid.configFile(), "setup=Invalid Identifier!\n");
            expectFailure(() -> StartConfig.selected(manager), "Invalid identifier rejected");
            Files.writeString(SimpleVoid.configFile(), "setup=\n");
        }
        Path overlay = temporary.resolve("pack");
        Path selector = overlay.resolve("data/simplevoid/simplevoid/world_setup.json");
        Files.createDirectories(selector.getParent());
        Files.writeString(selector, "{\"setup\":\"simplevoid:ship\"}");
        try (MultiPackResourceManager manager = manager(resources, overlay)) {
            check(StartConfig.selected(manager).equals(SimpleVoid.id("ship")), "Data pack selects ship");
            Files.writeString(SimpleVoid.configFile(), "setup=simplevoid:skyblock\n");
            check(StartConfig.selected(manager).equals(SimpleVoid.id("skyblock")), "Config overrides selector");
            Path override = SimpleVoid.configFile().getParent().resolve("simplevoid/world_setup/simplevoid/skyblock.json");
            Files.createDirectories(override.getParent());
            Files.copy(resources.resolve("data/simplevoid/simplevoid/world_setup/ship.json"), override);
            check(StartConfig.load(manager, SimpleVoid.id("skyblock")).spawnPlacement().structure().equals(SimpleVoid.id("ship")), "Config can define full setup");
            Files.delete(override);
        }
        Path custom = overlay.resolve("data/homebound/simplevoid/world_setup/ship.json");
        Files.createDirectories(custom.getParent());
        var homeboundJson = json(resources.resolve("data/simplevoid/simplevoid/world_setup/ship.json")).getAsJsonObject();
        homeboundJson.getAsJsonArray("placements").get(0).getAsJsonObject().addProperty("structure", "homebound:colony_ship");
        homeboundJson.getAsJsonArray("placements").get(0).getAsJsonObject().addProperty("rotation", "clockwise_90");
        Files.writeString(custom, homeboundJson.toString());
        Files.writeString(selector, "{\"setup\":\"homebound:ship\"}");
        Files.writeString(SimpleVoid.configFile(), "setup=\n");
        try (MultiPackResourceManager manager = manager(resources, overlay)) {
            var homebound = StartConfig.load(manager, StartConfig.selected(manager));
            check(homebound.spawnPlacement().structure().toString().equals("homebound:colony_ship"), "External ship structure ID");
            check(homebound.spawnPosition().equals(new BlockPos(-5, 65, 4)), "Spawn offset rotates with template");
            homeboundJson.getAsJsonArray("placements").add(homeboundJson.getAsJsonArray("placements").get(0).deepCopy());
            Files.writeString(custom, homeboundJson.toString());
            expectFailure(() -> StartConfig.load(manager, StartConfig.selected(manager)), "Duplicate placement rejected");
            homeboundJson.getAsJsonArray("placements").remove(1);
            homeboundJson.getAsJsonObject("spawn").addProperty("placement", "missing");
            Files.writeString(custom, homeboundJson.toString());
            expectFailure(() -> StartConfig.load(manager, StartConfig.selected(manager)), "Missing spawn placement rejected");
        }
        check(PlacementPosition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).error().isPresent(), "Missing position rejected");
        check(PlacementPosition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"fixed\":[0,64,0],\"spawn_offset\":[0,0,0]}")).error().isPresent(), "Ambiguous position rejected");
        var invalidSpawn = WorldSetup.CODEC.encodeStart(JsonOps.INSTANCE, skyblock).getOrThrow().getAsJsonObject();
        invalidSpawn.getAsJsonArray("placements").get(0).getAsJsonObject().add("position", JsonParser.parseString("{\"spawn_offset\":[0,0,0]}"));
        check(WorldSetup.CODEC.parse(JsonOps.INSTANCE, invalidSpawn).error().isPresent(), "Spawn-relative spawn structure rejected");
        var invalidName = StructurePlacement.CODEC.encodeStart(JsonOps.INSTANCE, skyblock.spawnPlacement()).getOrThrow().getAsJsonObject();
        invalidName.addProperty("name", "nested/name");
        check(StructurePlacement.CODEC.parse(JsonOps.INSTANCE, invalidName).error().isPresent(), "Ambiguous placement key names rejected");
        for (Rotation rotation : Rotation.values()) {
            check(Rotation.CODEC.parse(JsonOps.INSTANCE, Rotation.CODEC.encodeStart(JsonOps.INSTANCE, rotation).getOrThrow()).getOrThrow() == rotation, "Rotation round trip " + rotation);
        }
        var condition = PlacementCondition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"mod_loaded\":\"example\",\"score\":{\"objective\":\"test\",\"holder\":\"#world\",\"min\":2,\"max\":3}}")).getOrThrow();
        Scoreboard scoreboard = new Scoreboard();
        check(PlacementCondition.ALWAYS.test(id -> false, scoreboard), "Omitted condition passes");
        check(!condition.test(id -> true, scoreboard), "Missing objective fails condition");
        var objective = scoreboard.addObjective("test", ObjectiveCriteria.DUMMY, Component.literal("test"), ObjectiveCriteria.RenderType.INTEGER, false, null);
        check(!condition.test(id -> true, scoreboard), "Missing score fails condition");
        var score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("#world"), objective);
        score.set(2);
        check(condition.test(id -> true, scoreboard), "Score minimum inclusive");
        check(!condition.test(id -> false, scoreboard), "Mod condition AND score");
        score.set(4);
        check(!condition.test(id -> true, scoreboard), "Score maximum enforced");
        score.set(3);
        check(condition.test(id -> true, scoreboard), "Score maximum inclusive");
        check(!PlacementCondition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"enabled\":false}")).getOrThrow().test(id -> true, scoreboard), "Disabled condition skips");
        check(PlacementCondition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"score\":{\"objective\":\"test\",\"holder\":\"x\",\"min\":5,\"max\":1}}")).error().isPresent(), "Invalid score range rejected");
        for (String name : List.of("skyblock", "ship")) {
            WorldSetup setup = WorldSetup.CODEC.parse(JsonOps.INSTANCE,
                    json(resources.resolve("data/simplevoid/simplevoid/world_setup/" + name + ".json"))).getOrThrow();
            for (var placement : setup.placements()) {
                var tag = NbtIo.readCompressed(resources.resolve("data/simplevoid/structure/" + placement.structure().getPath() + ".nbt"), NbtAccounter.unlimitedHeap());
                StructureTemplate template = new StructureTemplate();
                template.load(BuiltInRegistries.BLOCK, tag);
                check(template.getSize().getY() > 0, placement.name() + " vanilla template loads");
                if (placement.name().equals(setup.spawn().placement())) {
                    var air = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.AIR);
                    check(air.stream().anyMatch(block -> block.pos().equals(setup.spawn().offset())), name + " spawn air");
                    check(air.stream().anyMatch(block -> block.pos().equals(setup.spawn().offset().above())), name + " headroom");
                }
            }
        }
        StartState state = StartState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        check(state.spawn().isEmpty() && state.placements().isEmpty(), "Fresh placement state");
        var setupId = SimpleVoid.id("skyblock");
        var key = skyblock.spawnPlacement().key(setupId);
        var snapshot = new StartState.Placed(SimpleVoid.id("island"), new BlockPos(0,64,0), Rotation.NONE);
        state.complete(key, snapshot);
        state.select(setupId, new StartState.SpawnPoint(skyblock.spawnPosition(), 0));
        check(state.isPlaced(key), "Placement marked");
        check(!state.isPlaced(skyblock.placement("distant_example").key(setupId)), "Other placement not marked");
        expectFailure(() -> state.complete(key, snapshot), "Duplicate marker rejected");
        JsonElement encoded = StartState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        StartState restored = StartState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        check(restored.placements().equals(state.placements()) && restored.spawn().equals(state.spawn()), "Placement state codec round trip");
        Path savedData = temporary.resolve("data");
        try (SavedDataStorage storage = new SavedDataStorage(savedData, DataFixers.getDataFixer(), lookup)) {
            storage.set(StartState.TYPE, state);
            storage.saveAndJoin();
        }
        try (SavedDataStorage storage = new SavedDataStorage(savedData, DataFixers.getDataFixer(), lookup)) {
            var saved = storage.computeIfAbsent(StartState.TYPE);
            check(saved.isPlaced(key) && saved.placements().get(key).equals(snapshot), "Placement marker survives disk reload");
            check(saved.selectedSetup().orElseThrow().equals(setupId), "Selected setup survives reload");
        }
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        VoidCommands.register(dispatcher);
        check(dispatcher.getRoot().getChild("void").getChild("place") != null, "Placement command registered");
        CommandDispatcher<CommandSourceStack> parser = new CommandDispatcher<>();
        parser.register(Commands.literal("void").then(dispatcher.getRoot().getChild("void").getChild("place")));
        for (String command : List.of("void place simplevoid:skyblock", "void place simplevoid:skyblock distant_example",
                "void place later_example", "void place homebound:ships/colony room")) {
            var parsed = parser.parse(command, null);
            check(!parsed.getReader().canRead() && parsed.getExceptions().isEmpty(), "Command parses " + command);
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
