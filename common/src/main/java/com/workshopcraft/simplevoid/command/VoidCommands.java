package com.workshopcraft.simplevoid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.workshopcraft.simplevoid.api.VoidPlacements;
import com.workshopcraft.simplevoid.start.StartConfig;
import com.workshopcraft.simplevoid.start.StartState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;

public final class VoidCommands {
    private VoidCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("void").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("place")
                        .then(Commands.argument("setup_or_placement", StringArgumentType.word())
                                .executes(context -> place(context, null))
                                .then(Commands.argument("placement", StringArgumentType.word())
                                        .executes(context -> place(context, StringArgumentType.getString(context, "placement")))))));
    }

    private static int place(CommandContext<CommandSourceStack> context, String named) {
        var source = context.getSource();
        String target = StringArgumentType.getString(context, "setup_or_placement");
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
