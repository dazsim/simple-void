package com.workshopcraft.simplevoid.mixin;

import com.workshopcraft.simplevoid.start.VoidStart;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void simplevoid$initialSpawn(ServerLevel level, ServerLevelData data, boolean bonusChest,
                                               boolean debug, LevelLoadListener listener, CallbackInfo callback) {
        if (!debug && VoidStart.initialize(level, data)) callback.cancel();
    }
}
