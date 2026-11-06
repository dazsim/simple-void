package com.workshopcraft.simplevoid.mixin;

import com.workshopcraft.simplevoid.start.VoidStart;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSpawnFinder.class)
public abstract class PlayerSpawnFinderMixin {
    @Inject(method = "findSpawn", at = @At("HEAD"), cancellable = true)
    private static void simplevoid$exactSpawn(ServerLevel level, BlockPos suggestion,
                                             CallbackInfoReturnable<CompletableFuture<Vec3>> callback) {
        VoidStart.exactSpawn(level, suggestion)
                .ifPresent(pos -> callback.setReturnValue(CompletableFuture.completedFuture(pos)));
    }
}
