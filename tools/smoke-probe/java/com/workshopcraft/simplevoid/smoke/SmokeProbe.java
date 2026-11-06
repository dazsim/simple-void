package com.workshopcraft.simplevoid.smoke;

import com.workshopcraft.simplebooks.quest.QuestActionPacket;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import com.workshopcraft.simplevoid.api.VoidAreas;
import com.workshopcraft.simplevoid.neoforge.AreaStateChangedEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("simplevoid_smoke")
public final class SmokeProbe {
    private int common;
    private int neoForge;

    public SmokeProbe() {
        VoidAreaEvents.listen(change -> common++);
        NeoForge.EVENT_BUS.addListener((AreaStateChangedEvent event) -> neoForge++);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(
                Commands.literal("simplevoidsmoke").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("status").executes(context -> {
                            var area = VoidAreas.getArea(context.getSource().getLevel(), "island");
                            context.getSource().sendSuccess(() -> Component.literal("events common=" + common + " neoforge=" + neoForge
                                    + " hooks=" + area.onStateChange().enabled()), false);
                            return 1;
                        }))
                        .then(Commands.literal("deny").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            var source = player.createCommandSourceStack();
                            boolean op = source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
                            String before = VoidAreas.getState(player.level(), "island");
                            var commands = context.getSource().getServer().getCommands();
                            String attempt = "void area set island nonop_forbidden";
                            var parsed = commands.getDispatcher().parse(attempt, source);
                            boolean denied = !commands.getDispatcher().getRoot().getChild("void").canUse(source) && parsed.getReader().canRead();
                            commands.performPrefixedCommand(source, attempt);
                            boolean unchanged = VoidAreas.getState(player.level(), "island").equals(before);
                            context.getSource().sendSuccess(() -> Component.literal("DENIED nonop=" + !op + " parser=" + denied + " unchanged=" + unchanged), false);
                            return !op && denied && unchanged ? 1 : 0;
                        })))
                        .then(Commands.literal("claim").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            boolean op = player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
                            if (op) throw new IllegalStateException("Claimant must not be an operator");
                            var result = new QuestActionPacket("claim", "simplevoid:smoke/repair", "", -1, "").handle(player);
                            player.sendSystemMessage(Component.literal("Simple Books: " + result.message()));
                            context.getSource().sendSuccess(() -> Component.literal("CLAIM ok=" + result.ok() + " nonop=" + !op + " " + result.message()), false);
                            return result.ok() ? 1 : 0;
                        })))));
    }
}
