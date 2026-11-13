package com.workshopcraft.simplevoid.validation;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.api.VoidArea;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import com.workshopcraft.simplevoid.area.AreaBounds;
import com.workshopcraft.simplevoid.area.AreaDefinition;
import com.workshopcraft.simplevoid.area.AreaHazard;
import com.workshopcraft.simplevoid.area.AreaHazards;
import com.workshopcraft.simplevoid.area.AreaHooks;
import com.workshopcraft.simplevoid.area.AreaSavedData;
import com.workshopcraft.simplevoid.area.HazeSpec;
import com.workshopcraft.simplevoid.command.VoidCommands;
import com.workshopcraft.simplevoid.hazard.HazardCatalog;
import com.workshopcraft.simplevoid.hazard.HazardEngine;
import com.workshopcraft.simplevoid.hazard.HazardServices;
import com.workshopcraft.simplevoid.hazard.WorkshopCoreHazardServices;
import com.workshopcraft.simplevoid.hazard.VoidConfig;
import com.workshopcraft.simplevoid.setup.StructurePlacement;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.simplevoid.start.StartState;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class HazardChecks {
    private static int checks;

    public static int run(Path temporary, WorldSetup ship, WorldSetup skyblock) throws Exception {
        checks = 0;
        String base = "{\"name\":\"cabin\",\"bounds\":{\"min\":[1,1,1],\"max\":[7,3,9]}}";
        var empty = AreaDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(base)).getOrThrow();
        check(!empty.hazards().defined(), "Hazards default to empty");
        var parsed = AreaDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"name":"cabin","bounds":{"min":[1,1,1],"max":[7,3,9]},"hazards":{"damaged":[
                {"type":"simplevoid:no_air"},{"type":"simplevoid:cold"},{"type":"simplevoid:damage","damage":2}]}}
                """)).getOrThrow();
        List<AreaHazard> damaged = parsed.hazards().forState("damaged");
        check(damaged.size() == 3, "Three built-in types parse");
        check(damaged.get(0).airPerTick() == 1 && damaged.get(0).damageType().equals(AreaHazard.NO_AIR), "no_air defaults");
        check(damaged.get(1).freezePerTick() == 3 && damaged.get(1).damage() == 1 && damaged.get(1).intervalTicks() == 20, "cold defaults");
        check(damaged.get(2).damage() == 2 && damaged.get(2).damageType().equals(AreaHazard.DAMAGE), "damage override");
        check(parsed.hazards().forState("repaired").isEmpty(), "Omitted state has no hazards");
        rejects(() -> AreaDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                base.replace("}", ",\"hazards\":{\"damaged\":[{\"type\":\"simplevoid:gas\"}]}}"))).getOrThrow(), "Unknown type rejected");
        StringBuilder tooMany = new StringBuilder(base.substring(0, base.length() - 1) + ",\"hazards\":{\"damaged\":[");
        for (int i = 0; i < 9; i++) tooMany.append(i == 0 ? "" : ",").append("{\"type\":\"simplevoid:damage\"}");
        tooMany.append("]}}");
        rejects(() -> AreaDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(tooMany.toString())).getOrThrow(), "Oversized hazard list rejected");
        rejects(() -> HazeSpec.decodeColor("blue"), "Invalid haze color rejected");

        AreaHazard soft = AreaHazard.create(AreaHazard.DAMAGE, java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Optional.of(1.0F), java.util.Optional.of(40), java.util.Optional.empty(), java.util.Optional.empty());
        AreaHazard hard = AreaHazard.create(AreaHazard.DAMAGE, java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Optional.of(3.0F), java.util.Optional.of(10), java.util.Optional.empty(),
                java.util.Optional.of(new HazeSpec(HazeSpec.decodeColor("#80FF0000"), 0.8F, 5)));
        AreaHazard merged = HazardEngine.merge(List.of(soft, hard)).getFirst();
        check(merged.damage() == 3.0F && merged.intervalTicks() == 10 && merged.hazeOrDefault().density() == 0.8F, "Overlap keeps harsher values");

        AreaBounds bounds = new AreaBounds(new BlockPos(1, 64, 1), new BlockPos(7, 66, 9));
        check(HazardEngine.intersects(bounds, new AABB(1.1, 64.1, 1.1, 1.4, 65.8, 1.4)), "Box inside area counts as present");
        check(HazardEngine.intersects(bounds, new AABB(7.0, 66.0, 9.0, 7.4, 67.0, 9.4)), "Box on inclusive max edge counts as present");
        check(!HazardEngine.intersects(bounds, new AABB(8.0, 64.0, 1.0, 8.5, 65.0, 2.0)), "Box outside area is absent");

        var shipHazards = ship.spawnPlacement().areas().getFirst().hazards();
        check(shipHazards.forState("damaged").size() == 3, "Demo ship cabin has sample hazards");
        check(shipHazards.forState("repaired").isEmpty(), "Demo ship repaired state has no hazards");
        check(!skyblock.spawnPlacement().areas().getFirst().hazards().defined(), "Skyblock island has no hazards");

        Identifier placement = Identifier.parse("simplevoid:ship/ship");
        VoidArea cabin = VoidArea.create(placement, new StartState.Placed(SimpleVoid.id("ship"), new BlockPos(0, 64, 0), Rotation.NONE),
                ship.spawnPlacement().areas().getFirst());
        var loader = (java.util.function.Function<Identifier, WorldSetup>) id -> ship;
        check(HazardCatalog.defined(cabin, loader).forState("damaged").size() == 3, "Live lookup reads current setup");
        var encoded = com.workshopcraft.simplevoid.api.VoidArea.CODEC.encodeStart(JsonOps.INSTANCE, cabin).getOrThrow().toString();
        check(!encoded.contains("no_air") && !encoded.contains("hazards"), "Hazards are not snapshotted into saved areas");
        AreaSavedData data = AreaSavedData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{} ")).getOrThrow();
        data.importPlacement(placement, List.of(cabin));
        var originalCabin = ship.spawnPlacement().areas().getFirst();
        var retunedHazard = AreaHazard.create(AreaHazard.NO_AIR, java.util.Optional.of(4), java.util.Optional.empty(),
                java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty());
        var rest = originalCabin.hazards().forState("damaged").stream().skip(1).toList();
        var retunedCabin = new AreaDefinition(originalCabin.name(), originalCabin.bounds(), originalCabin.initialState(),
                originalCabin.onStateChange(), new AreaHazards(java.util.Map.of("damaged", java.util.stream.Stream.concat(java.util.stream.Stream.of(retunedHazard), rest.stream()).toList())));
        var originalPlacement = ship.spawnPlacement();
        var updatedPlacement = new StructurePlacement(originalPlacement.name(), originalPlacement.structure(), originalPlacement.position(),
                originalPlacement.rotation(), originalPlacement.atCreation(), originalPlacement.condition(), java.util.List.of(retunedCabin));
        WorldSetup updated = new WorldSetup(ship.spawn(), java.util.List.of(updatedPlacement));
        var afterTune = HazardCatalog.defined(data.require("cabin").withState("damaged"), id -> updated).forState("damaged");
        check(afterTune.getFirst().airPerTick() == 4 && data.require("cabin").state().equals("damaged"), "Live retune does not reset area state");
        check(HazardCatalog.defined(data.require("cabin").withState("repaired"), loader).forState("repaired").isEmpty(), "Repaired state clears active hazards");

        check(HazardEngine.shouldApplyEffects(true, false, false), "Survival takes effects");
        check(!HazardEngine.shouldApplyEffects(true, true, false), "Creative skips effects");
        check(!HazardEngine.shouldApplyEffects(true, false, true), "Spectator skips effects");
        check(!HazardEngine.shouldApplyEffects(false, false, false), "Master switch disables effects");
        check(HazardEngine.shouldShowHaze(true, true, false) && !HazardEngine.shouldShowHaze(true, true, true), "Spectator skips haze, creative does not");
        check(HazardEngine.overlay(shipHazards.forState("damaged")) != null, "Active hazards pick a haze overlay");

        check(Class.forName("com.workshopcraft.workshopcore.hazard.HazardDamage") != null, "Workshop Core hazard API is on the classpath");
        check(HazardServices.get() instanceof WorkshopCoreHazardServices, "Core-backed hazard services are active");
        check(com.workshopcraft.workshopcore.hazard.HazardDamage.key(AreaHazard.NO_AIR).identifier().equals(AreaHazard.NO_AIR), "HazardDamage.key uses our damage type ids");

        SimpleVoid.initialize(temporary.resolve("hazard-config"));
        Files.createDirectories(SimpleVoid.configFile().getParent());
        if (Files.exists(SimpleVoid.configFile())) Files.delete(SimpleVoid.configFile());
        VoidConfig.reload();
        check(VoidConfig.hazardsEnabled() && VoidConfig.hazeEnabled(), "Missing config defaults to hazards on");
        Files.writeString(SimpleVoid.configFile(), "setup=simplevoid:ship\n", StandardCharsets.UTF_8);
        VoidConfig.reload();
        check(VoidConfig.hazardsEnabled() && VoidConfig.hazeEnabled(), "Setup-only config keeps hazard defaults");
        Files.writeString(SimpleVoid.configFile(), "hazards=false\nhaze=false\n", StandardCharsets.UTF_8);
        VoidConfig.reload();
        check(!VoidConfig.hazardsEnabled() && !VoidConfig.hazeEnabled(), "Config can disable hazards and haze");
        Files.writeString(SimpleVoid.configFile(), "setup=\nhazards=true\nhaze=true\n", StandardCharsets.UTF_8);
        VoidConfig.reload();

        AtomicInteger events = new AtomicInteger();
        var subscription = VoidAreaEvents.listenHazards(change -> {
            events.incrementAndGet();
            check(change.previous().isEmpty() && change.current().size() == 3, "Presence event carries previous and current hazards");
        });
        VoidAreaEvents.post(new VoidAreaEvents.HazardPresence(null, null, List.of(), shipHazards.forState("damaged")));
        check(events.get() == 1, "Hazard presence listener fires");
        subscription.close();
        VoidAreaEvents.post(new VoidAreaEvents.HazardPresence(null, null, List.of(), shipHazards.forState("damaged")));
        check(events.get() == 1, "Hazard presence listener unsubscribes");

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        VoidCommands.register(dispatcher);
        var source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null,
                LevelBasedPermissionSet.forLevel(PermissionLevel.byId(2)), "checks", Component.literal("checks"), null, null);
        var parsedCommand = dispatcher.parse("void area hazards cabin", source);
        check(!parsedCommand.getReader().canRead() && parsedCommand.getExceptions().isEmpty(), "Hazards command parses");
        var denied = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null,
                LevelBasedPermissionSet.forLevel(PermissionLevel.byId(1)), "checks", Component.literal("checks"), null, null);
        var blocked = dispatcher.parse("void area hazards cabin", denied);
        check(blocked.getReader().canRead() || !blocked.getExceptions().isEmpty() || !dispatcher.getRoot().getChild("void").canUse(denied), "Hazards command needs permission 2");

        System.out.println("Passed " + checks + " hazard checks. Workshop Core 0.5.0 damage, wear and haze are wired.");
        return checks;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static void rejects(Runnable action, String message) {
        try { action.run(); } catch (RuntimeException expected) { checks++; return; }
        throw new AssertionError(message);
    }
}
