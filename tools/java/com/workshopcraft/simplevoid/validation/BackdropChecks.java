package com.workshopcraft.simplevoid.validation;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.api.VoidBackdrops;
import com.workshopcraft.simplevoid.backdrop.BackdropHooks;
import com.workshopcraft.simplevoid.command.VoidCommands;
import com.workshopcraft.simplevoid.setup.WorldSetup;
import com.workshopcraft.workshopcore.backdrop.Backdrop;
import com.workshopcraft.workshopcore.backdrop.BackdropLayer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class BackdropChecks {
    private static int checks;

    public static int run(Path resources, WorldSetup ship, WorldSetup skyblock, WorldSetup skyblockPortal) throws Exception {
        checks = 0;
        Backdrop stars = parseBackdrop(resources, "space_stars");
        Backdrop warp = parseBackdrop(resources, "space_warp");
        check(stars.layers().size() >= 2 && stars.transition().durationTicks() == 80, "space_stars parses with a transition");
        check(stars.layers().stream().anyMatch(layer -> layer instanceof BackdropLayer.Stars), "space_stars has a stars layer");
        check(stars.layers().stream().noneMatch(layer -> layer instanceof BackdropLayer.Warp), "space_stars has no warp layer");
        BackdropLayer.Warp warpLayer = warp.layers().stream().filter(layer -> layer instanceof BackdropLayer.Warp)
                .map(layer -> (BackdropLayer.Warp) layer).findFirst().orElseThrow();
        check(warpLayer.direction().yaw() == 0.0F && warpLayer.direction().pitch() == 0.0F, "space_warp points toward ship forward (-Z)");
        check(Class.forName("com.workshopcraft.workshopcore.backdrop.Backdrops") != null, "Workshop Core backdrop API is on the classpath");

        check(ship.backdrop().equals(Optional.of(SimpleVoid.id("space_stars"))), "Ship setup defaults to space_stars");
        check(skyblock.backdrop().isEmpty() && skyblock.onBackdropChange().equals(BackdropHooks.NONE), "Skyblock has no default backdrop");
        check(skyblockPortal.backdrop().isEmpty(), "Skyblock with portal has no default backdrop");
        check(Files.notExists(resources.resolve("data/simplevoid/workshopcore/backdrop_defaults.json")),
                "No global Workshop Core backdrop_defaults file (skyblock must stay vanilla)");

        var hooks = BackdropHooks.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"enabled":true,"commands":{
                  "simplevoid:space_stars":["say stars {backdrop} from {previous} in {dimension}"],
                  "simplevoid:space_warp":["say warp"],
                  "vanilla":["say clear"]
                }}
                """)).getOrThrow();
        check(hooks.forBackdrop(Optional.of(SimpleVoid.id("space_stars"))).size() == 1, "Hooks run for the new backdrop id");
        check(hooks.forBackdrop(Optional.empty()).equals(List.of("say clear")), "Vanilla key is used when the sky is cleared");
        String expanded = BackdropHooks.expand(hooks.forBackdrop(Optional.of(SimpleVoid.id("space_stars"))).getFirst(),
                Optional.of(SimpleVoid.id("space_stars")), Optional.of(SimpleVoid.id("space_warp")), Identifier.parse("minecraft:overworld"));
        check(expanded.equals("say stars simplevoid:space_stars from simplevoid:space_warp in minecraft:overworld"),
                "Hook placeholders expand");
        check(new BackdropHooks(false, hooks.commands()).forBackdrop(Optional.of(SimpleVoid.id("space_stars"))).isEmpty(),
                "Disabled hooks do not run");
        rejects(() -> BackdropHooks.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"enabled\":true,\"commands\":{\"stars\":[\"say hi\"]}}")).getOrThrow(), "Unnamespaced hook key rejected");
        rejects(() -> BackdropHooks.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"enabled\":true,\"commands\":{\"vanilla\":[\"\"]}}")).getOrThrow(), "Blank hook command rejected");

        var withHooks = WorldSetup.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"spawn":{"placement":"start","offset":[4,3,2]},"placements":[
                  {"name":"start","structure":"simplevoid:island","position":{"fixed":[0,64,0]}}],
                 "backdrop":"simplevoid:space_warp",
                 "on_backdrop_change":{"enabled":true,"commands":{"simplevoid:space_warp":["say warp"]}}}
                """)).getOrThrow();
        check(withHooks.backdrop().equals(Optional.of(SimpleVoid.id("space_warp")))
                && withHooks.onBackdropChange().forBackdrop(Optional.of(SimpleVoid.id("space_warp"))).equals(List.of("say warp")),
                "World setup parses a default backdrop and hooks");

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        VoidCommands.register(dispatcher);
        var allowed = source(2);
        for (String command : List.of("void backdrop get", "void backdrop set simplevoid:space_stars",
                "void backdrop set simplevoid:space_warp 80", "void backdrop clear", "void backdrop clear 0")) {
            var parsed = dispatcher.parse(command, allowed);
            check(!parsed.getReader().canRead() && parsed.getExceptions().isEmpty(), "Backdrop command parses: " + command);
        }
        var denied = source(1);
        check(!dispatcher.getRoot().getChild("void").canUse(denied), "Backdrop commands need permission 2");
        var unknown = dispatcher.parse("void backdrop set simplevoid:missing", allowed);
        check(!unknown.getReader().canRead() && unknown.getExceptions().isEmpty(), "Unknown id is accepted at parse time");

        System.out.println("Passed " + checks + " backdrop checks. Workshop Core 0.5.0 backdrops are wrapped.");
        return checks;
    }

    private static Backdrop parseBackdrop(Path resources, String name) throws Exception {
        Path path = resources.resolve("data/simplevoid/workshopcore/backdrops/" + name + ".json");
        try (var reader = Files.newBufferedReader(path)) {
            return Backdrop.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
        }
    }

    private static CommandSourceStack source(int permission) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null,
                LevelBasedPermissionSet.forLevel(PermissionLevel.byId(permission)), "checks", Component.literal("checks"), null, null);
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
