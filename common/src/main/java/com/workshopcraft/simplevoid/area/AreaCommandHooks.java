package com.workshopcraft.simplevoid.area;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.api.VoidArea;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec3;

public final class AreaCommandHooks {
    private AreaCommandHooks() {}

    public static ParseResults<CommandSourceStack> parse(CommandDispatcher<CommandSourceStack> dispatcher,
                                                        CommandSourceStack source, String command) {
        var parsed = dispatcher.parse(Commands.trimOptionalPrefix(command), source);
        try {
            Commands.validateParseResults(parsed);
        } catch (CommandSyntaxException exception) {
            throw new IllegalArgumentException("Invalid area command: " + exception.getMessage(), exception);
        }
        return parsed;
    }

    public static void run(ServerLevel level, VoidArea area, String command) {
        var source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPosition(Vec3.atLowerCornerOf(area.origin()))
                .withPermission(LevelBasedPermissionSet.forLevel(PermissionLevel.byId(2))).withSuppressedOutput()
                .withCallback((success, result) -> {
                    if (!success) LogUtils.getLogger().warn("Simple Void area command did not succeed for {}: {}", area.id(), command);
                });
        var commands = level.getServer().getCommands();
        commands.performCommand(parse(commands.getDispatcher(), source, command), Commands.trimOptionalPrefix(command));
    }
}
