package com.workshopcraft.simplevoid.backdrop;

import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.area.AreaCommandHooks;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

public final class BackdropCommandHooks {
    private BackdropCommandHooks() {}

    public static void run(ServerLevel level, String command) {
        var source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPermission(LevelBasedPermissionSet.forLevel(PermissionLevel.byId(2))).withSuppressedOutput()
                .withCallback((success, result) -> {
                    if (!success) LogUtils.getLogger().warn("Simple Void backdrop command did not succeed: {}", command);
                });
        var commands = level.getServer().getCommands();
        commands.performCommand(AreaCommandHooks.parse(commands.getDispatcher(), source, command), Commands.trimOptionalPrefix(command));
    }
}
