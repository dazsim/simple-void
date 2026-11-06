package com.workshopcraft.simplevoid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.workshopcraft.simplevoid.api.VoidPlacements;
import com.workshopcraft.simplevoid.api.VoidAreas;
import com.workshopcraft.simplevoid.start.StartConfig;
import com.workshopcraft.simplevoid.start.StartState;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;

public final class VoidCommands {
    private VoidCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("void").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("area")
                        .then(Commands.literal("list").executes(context -> areas(context, "list")))
                        .then(Commands.literal("get").then(Commands.argument("name", IdentifierArgument.id())
                                .suggests(VoidCommands::suggestAreas).executes(context -> areas(context, "get"))))
                        .then(Commands.literal("set").then(Commands.argument("name", IdentifierArgument.id())
                                .suggests(VoidCommands::suggestAreas)
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("damaged", "repaired"), builder))
                                        .executes(context -> areas(context, "set"))))))
                .then(Commands.literal("place")
                        .then(Commands.argument("setup_or_placement", IdentifierArgument.id())
                                .executes(context -> place(context, null))
                                .then(Commands.argument("placement", StringArgumentType.word())
                                        .executes(context -> place(context, StringArgumentType.getString(context, "placement")))))));
    }

    private static CompletableFuture<Suggestions> suggestAreas(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        var areas = VoidAreas.listAreas(context.getSource().getLevel());
        return SharedSuggestionProvider.suggest(areas.stream().flatMap(area -> Stream.of(area.name(), area.id().toString())), builder);
    }

    private static String argumentText(CommandContext<CommandSourceStack> context, String name) {
        return context.getNodes().stream().filter(node -> node.getNode().getName().equals(name))
                .map(node -> node.getRange().get(context.getInput())).findFirst().orElseThrow();
    }

    private static int areas(CommandContext<CommandSourceStack> context, String action) {
        var source = context.getSource();
        try {
            if (action.equals("list")) {
                var list = VoidAreas.listAreas(source.getLevel());
                source.sendSuccess(() -> Component.literal("Simple Void: " + list.size() + " areas in " + source.getLevel().dimension().identifier()), false);
                for (var area : list) source.sendSuccess(() -> Component.literal(area.id() + " = " + area.state()), false);
                return list.size();
            }
            String name = argumentText(context, "name");
            var area = VoidAreas.getArea(source.getLevel(), name);
            if (action.equals("get")) {
                source.sendSuccess(() -> Component.literal(area.id() + " = " + area.state() + "; bounds "
                        + area.bounds().min().toShortString() + " to " + area.bounds().max().toShortString()), false);
                return 1;
            }
            String state = StringArgumentType.getString(context, "state");
            boolean changed = VoidAreas.setState(source.getLevel(), area.id(), state);
            source.sendSuccess(() -> Component.literal(area.id() + " = " + state + (changed ? " (changed)" : " (unchanged)")), true);
            return 1;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("Simple Void: " + exception.getMessage()));
            return 0;
        }
    }

    private static int place(CommandContext<CommandSourceStack> context, String named) {
        var source = context.getSource();
        String target = argumentText(context, "setup_or_placement");
        try {
            Identifier setup;
            String placement = named;
            if (target.contains(":")) {
                setup = Identifier.parse(target);
            } else {
                if (named != null) throw new IllegalArgumentException("Use a namespaced setup id before a placement name");
                setup = StartState.get(source.getLevel()).selectedSetup()
                        .orElseGet(() -> StartConfig.selected(source.getServer().getResourceManager()));
                placement = target;
            }
            var result = placement == null ? VoidPlacements.placeSetup(source.getLevel(), setup)
                    : VoidPlacements.placePlacement(source.getLevel(), setup, placement);
            source.sendSuccess(() -> Component.literal("Simple Void: placed " + result.placed().size()
                    + ", already placed " + result.alreadyPlaced().size() + ", conditions not met " + result.conditionFailed().size()), true);
            return result.placed().size();
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("Simple Void: " + exception.getMessage()));
            return 0;
        }
    }
}
