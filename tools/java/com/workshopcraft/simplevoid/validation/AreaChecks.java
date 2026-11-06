package com.workshopcraft.simplevoid.validation;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.api.VoidArea;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import com.workshopcraft.simplevoid.area.AreaBounds;
import com.workshopcraft.simplevoid.area.AreaCommandHooks;
import com.workshopcraft.simplevoid.area.AreaDefinition;
import com.workshopcraft.simplevoid.area.AreaEngine;
import com.workshopcraft.simplevoid.area.AreaHooks;
import com.workshopcraft.simplevoid.area.AreaSavedData;
import com.workshopcraft.simplevoid.area.AreaTransitions;
import com.workshopcraft.simplevoid.command.VoidCommands;
import com.workshopcraft.simplevoid.setup.StructurePlacement;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.simplevoid.start.StartState;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class AreaChecks {
    private static int checks;

    public static int run(Path temporary, HolderLookup.Provider lookup, WorldSetup skyblock) throws Exception {
        checks = 0;
        String base = "{\"name\":\"bridge\",\"bounds\":{\"min\":[1,1,1],\"max\":[4,3,6]}}";
        var definition = AreaDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(base)).getOrThrow();
        check(definition.initialState().equals("damaged"), "Default damaged state");
        check(!definition.onStateChange().enabled(), "Hooks are opt-in");
        var placementJson = StructurePlacement.CODEC.encodeStart(JsonOps.INSTANCE, skyblock.spawnPlacement()).getOrThrow().getAsJsonObject();
        placementJson.remove("areas");
        check(StructurePlacement.CODEC.parse(JsonOps.INSTANCE, placementJson).getOrThrow().areas().isEmpty(), "M1 setup without areas loads");
        for (String name : List.of("bad/name", "Bad", "", "bad name")) {
            var bad = JsonParser.parseString(base).getAsJsonObject();
            bad.addProperty("name", name);
            rejects(() -> AreaDefinition.CODEC.parse(JsonOps.INSTANCE, bad).getOrThrow(), "Invalid area name " + name);
        }
        for (String state : List.of("Damaged", "", "repair @s", "a".repeat(65))) {
            rejects(() -> AreaDefinition.requireState(state), "Invalid state " + state);
        }
        check(AreaDefinition.requireState("pressurised.phase-2").equals("pressurised.phase-2"), "Extensible state tokens");
        rejects(() -> new AreaBounds(new BlockPos(2,0,0), BlockPos.ZERO), "Reversed bounds rejected");
        rejects(() -> new AreaDefinition("large", new AreaBounds(BlockPos.ZERO, new BlockPos(4096,0,0)), "damaged", AreaHooks.NONE), "Oversized bounds rejected");
        rejects(() -> new AreaDefinition("distant", new AreaBounds(new BlockPos(5000,0,0), new BlockPos(5001,0,0)), "damaged", AreaHooks.NONE), "Out of range local bounds rejected");
        var duplicate = StructurePlacement.CODEC.encodeStart(JsonOps.INSTANCE, skyblock.spawnPlacement()).getOrThrow().getAsJsonObject();
        duplicate.getAsJsonArray("areas").add(duplicate.getAsJsonArray("areas").get(0).deepCopy());
        rejects(() -> StructurePlacement.CODEC.parse(JsonOps.INSTANCE, duplicate).getOrThrow(), "Duplicate area names rejected");
        rejects(() -> new AreaHooks(true, Map.of("repaired", List.of("say one\nsay two"))), "Multiline command rejected");
        rejects(() -> new AreaHooks(true, Map.of("repaired", List.of(" "))), "Empty command rejected");
        rejects(() -> new AreaHooks(true, Map.of("bad state", List.of("say test"))), "Hook state validated");
        AreaBounds local = new AreaBounds(new BlockPos(1,2,3), new BlockPos(4,5,7));
        BlockPos origin = new BlockPos(10,64,30);
        List<AreaBounds> expected = List.of(
                new AreaBounds(new BlockPos(11,66,33), new BlockPos(14,69,37)),
                new AreaBounds(new BlockPos(3,66,31), new BlockPos(7,69,34)),
                new AreaBounds(new BlockPos(6,66,23), new BlockPos(9,69,27)),
                new AreaBounds(new BlockPos(13,66,26), new BlockPos(17,69,29)));
        for (Rotation rotation : Rotation.values()) {
            AreaBounds world = local.transform(origin, rotation);
            check(world.equals(expected.get(rotation.ordinal())), "World bounds " + rotation);
            for (int x : List.of(1,4)) for (int y : List.of(2,5)) for (int z : List.of(3,7)) {
                check(world.contains(StructureTemplate.transform(new BlockPos(x,y,z), Mirror.NONE, rotation, BlockPos.ZERO).offset(origin)), "Rotated corner inside " + rotation);
            }
            check(!world.contains(world.min().below()), "Bounds exclude outside block " + rotation);
            check(world.contains(world.min()) && world.contains(world.max()), "Bounds inclusive " + rotation);
        }
        AreaSavedData data = fresh();
        check(data.list().isEmpty(), "Fresh world has no areas");
        StartState m1 = StartState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{} ")).getOrThrow();
        var key = skyblock.spawnPlacement().key(SimpleVoid.id("skyblock"));
        var actual = new StartState.Placed(SimpleVoid.id("island"), new BlockPos(100,70,200), Rotation.CLOCKWISE_90);
        m1.complete(key, actual);
        AreaEngine.importExisting(m1, data, id -> skyblock);
        check(data.list().size() == 1 && data.imported(key), "M1 placed structure gains areas");
        VoidArea island = data.require("island");
        check(island.origin().equals(actual.position()) && island.rotation() == actual.rotation(), "Migration uses actual placed snapshot");
        check(island.bounds().equals(skyblock.spawnPlacement().areas().getFirst().bounds().transform(actual.position(), actual.rotation())), "Migrated area follows placed structure");
        check(data.list().stream().noneMatch(area -> area.name().equals("deferred_pad")), "Unplaced conditional areas absent");
        AreaEngine.importExisting(m1, data, id -> { throw new AssertionError("Already imported placement must not reread data"); });
        check(data.list().size() == 1, "Import is idempotent");
        check(data.require(island.id()).equals(island), "Canonical lookup");
        rejects(() -> data.list().clear(), "API snapshots are immutable");
        var hook = new AreaHooks(true, Map.of("repaired", List.of(
                "say {area} {previous} {state} {x} {y} {z} {rotation}", "say second"), "sealed", List.of("say sealed")));
        var hookedDefinition = new AreaDefinition("bridge", local, "damaged", hook);
        var shipKey = net.minecraft.resources.Identifier.parse("homebound:ships/colony/ship");
        VoidArea bridge = VoidArea.create(shipKey, new StartState.Placed(SimpleVoid.id("ship"), origin, Rotation.CLOCKWISE_90), hookedDefinition);
        data.importPlacement(shipKey, List.of(bridge));
        check(data.require("bridge").id().toString().equals("homebound:ships/colony/ship/bridge"), "Nested setup area identity");
        var otherKey = SimpleVoid.id("other/ship");
        VoidArea other = VoidArea.create(otherKey, actual, new AreaDefinition("bridge", local, "damaged", AreaHooks.NONE));
        data.importPlacement(otherKey, List.of(other));
        rejects(() -> data.require("bridge"), "Ambiguous short name rejected");
        rejects(() -> data.require("missing"), "Missing area rejected");
        List<String> commands = new ArrayList<>();
        AtomicInteger events = new AtomicInteger();
        AtomicInteger laterListeners = new AtomicInteger();
        var subscription = VoidAreaEvents.listen(event -> {
            events.incrementAndGet();
            check(event.before().state().equals("damaged") && event.after().state().equals("repaired"), "Event before and after states");
            check(data.require(bridge.id()).state().equals("repaired"), "State committed before event");
            check(commands.isEmpty(), "Event before command hooks");
            rejects(() -> AreaTransitions.set(data, other.id(), "loop", ignored -> {}, (area, command) -> {}), "Nested state changes blocked");
        });
        var broken = VoidAreaEvents.listen(event -> { throw new IllegalStateException("Expected failing test listener"); });
        var later = VoidAreaEvents.listen(event -> laterListeners.incrementAndGet());
        data.setDirty(false);
        boolean changed = AreaTransitions.set(data, bridge.id(), "repaired",
                change -> VoidAreaEvents.post(new VoidAreaEvents.StateChange(null, change.before(), change.after())),
                (area, command) -> commands.add(command));
        check(changed && data.isDirty(), "State change marks saved data dirty");
        check(events.get() == 1 && laterListeners.get() == 1, "Listener exceptions isolated");
        check(data.require(other.id()).state().equals("damaged"), "Reentrant change leaves other state intact");
        check(commands.equals(List.of("say homebound:ships/colony/ship/bridge damaged repaired 10 64 30 clockwise_90", "say second")), "Hook tokens and command order");
        data.setDirty(false);
        check(!AreaTransitions.set(data, bridge.id(), "repaired", change -> { throw new AssertionError("Duplicate event"); }, (area, command) -> { throw new AssertionError("Duplicate hook"); }), "Same state is a no-op");
        check(!data.isDirty() && commands.size() == 2 && events.get() == 1, "No-op does not dirty, fire or rerun hooks");
        subscription.close(); broken.close(); later.close();
        VoidAreaEvents.post(new VoidAreaEvents.StateChange(null, bridge, data.require(bridge.id())));
        check(events.get() == 1 && laterListeners.get() == 1, "Listeners unsubscribe");
        int count = commands.size();
        AreaTransitions.set(data, island.id(), "repaired", ignored -> {}, (area, command) -> commands.add(command));
        check(commands.size() == count, "Disabled hooks do not execute");
        rejects(() -> AreaTransitions.set(data, bridge.id(), "bad state", ignored -> {}, (area, command) -> {}), "State mutation validation");
        check(data.require(bridge.id()).state().equals("repaired"), "Rejected state leaves old value");
        AtomicInteger executed = new AtomicInteger();
        AreaTransitions.set(data, bridge.id(), "damaged", ignored -> {}, (area, command) -> executed.incrementAndGet());
        AreaTransitions.set(data, bridge.id(), "repaired", ignored -> {}, (area, command) -> {
            if (command.equals("say second")) executed.incrementAndGet();
            else throw new IllegalStateException("Expected failing test hook");
        });
        check(executed.get() == 1 && data.require(bridge.id()).state().equals("repaired"), "Hook failure preserves state and runs remaining hooks");
        AreaTransitions.set(data, bridge.id(), "sealed", ignored -> {}, (area, command) -> commands.add(command));
        check(data.require(bridge.id()).state().equals("sealed") && commands.getLast().equals("say sealed"), "Custom state and hook");
        data.importPlacement(shipKey, List.of(bridge));
        check(data.require(bridge.id()).state().equals("sealed"), "Repeated import preserves changed state");
        AreaTransitions.set(data, bridge.id(), "repaired", ignored -> {}, (area, command) ->
                AreaTransitions.set(data, other.id(), "sealed", ignored -> {}, (target, text) -> {}));
        check(data.require(other.id()).state().equals("sealed"), "Hooks can chain transitions outside listener scope");
        AreaTransitions.set(data, bridge.id(), "sealed", ignored -> {}, (area, command) -> {});
        AtomicInteger duplicateCalls = new AtomicInteger();
        java.util.function.Consumer<VoidAreaEvents.StateChange> shared = event -> duplicateCalls.incrementAndGet();
        var firstRegistration = VoidAreaEvents.listen(shared);
        var secondRegistration = VoidAreaEvents.listen(shared);
        firstRegistration.close(); firstRegistration.close();
        VoidAreaEvents.post(new VoidAreaEvents.StateChange(null, bridge, data.require(bridge.id())));
        check(duplicateCalls.get() == 1, "Closing one duplicate subscription twice leaves other registration");
        secondRegistration.close();
        var encoded = AreaSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        AreaSavedData decoded = AreaSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        check(decoded.list().equals(data.list()) && decoded.imported(shipKey), "Area codec round trip with hooks and import markers");
        try (SavedDataStorage storage = new SavedDataStorage(temporary.resolve("area-data"), DataFixers.getDataFixer(), lookup)) {
            storage.set(AreaSavedData.TYPE, data);
            storage.set(StartState.TYPE, m1);
            storage.saveAndJoin();
        }
        try (SavedDataStorage storage = new SavedDataStorage(temporary.resolve("area-data"), DataFixers.getDataFixer(), lookup)) {
            AreaSavedData saved = storage.computeIfAbsent(AreaSavedData.TYPE);
            check(saved.list().equals(data.list()) && saved.require(bridge.id()).state().equals("sealed"), "Area states, bounds and hooks survive disk reload");
            check(saved.imported(shipKey) && storage.computeIfAbsent(StartState.TYPE).placements().get(key).equals(actual), "Area import markers and M1 placement data survive reload");
            check(saved.require(bridge.id()).bounds().equals(bridge.bounds()), "Rotation survives reload");
        }
        check(fresh().list().isEmpty(), "Saved states isolated between worlds/dimensions");
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        VoidCommands.register(dispatcher);
        for (int permission = 0; permission <= 4; permission++) {
            var source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null,
                    LevelBasedPermissionSet.forLevel(PermissionLevel.byId(permission)), "checks", Component.literal("checks"), null, null);
            check(dispatcher.getRoot().getChild("void").canUse(source) == (permission >= 2), "Area command permission level " + permission);
            for (String command : List.of("void area list", "void area get island", "void area get homebound:ships/colony/ship/bridge",
                    "void area set island repaired", "void area set homebound:ships/colony/ship/bridge pressurised.phase-2")) {
                if (permission >= 2) {
                    check(!AreaCommandHooks.parse(dispatcher, source, "/" + command).getReader().canRead(), "Hook parser accepts valid optional slash " + command);
                } else {
                    rejects(() -> AreaCommandHooks.parse(dispatcher, source, command), "Hook parser respects source permission");
                }
                var parsed = dispatcher.parse(command, source);
                check((!parsed.getReader().canRead() && parsed.getExceptions().isEmpty()) == (permission >= 2), "Command parsing and permission " + permission + " " + command);
            }
        }
        var source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null,
                LevelBasedPermissionSet.forLevel(PermissionLevel.byId(2)), "checks", Component.literal("checks"), null, null);
        rejects(() -> AreaCommandHooks.parse(dispatcher, source, "void missing"), "Hook parse errors surface instead of being suppressed");
        rejects(() -> AreaCommandHooks.parse(dispatcher, source, "unknown-command"), "Unknown hook command rejected");
        System.out.println("Passed " + checks + " area checks, including expected failing-listener and failing-hook isolation cases.");
        return checks;
    }

    private static AreaSavedData fresh() {
        return AreaSavedData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{} ")).getOrThrow();
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
