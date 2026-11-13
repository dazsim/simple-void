package com.workshopcraft.simplevoid.hazard;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * TODO: replace with Workshop Core 0.5.0 once
 * {@code com.workshopcraft.workshopcore:workshopcore-neoforge-26.1.2:0.5.0}
 * is published to {@code /workspace/mods/workshopcore-maven}.
 * Call {@code HazardDamage.hurt}, {@code HazardProtection.isProtected},
 * {@code HazardProtection.tick}, {@code Haze.set} and {@code Haze.clear}.
 * Do not implement a local haze renderer or gear-wear logic here.
 */
public final class StubHazardServices implements HazardServices {
    @Override
    public boolean isProtected(Player player, Identifier hazard) {
        return false;
    }

    @Override
    public boolean tickProtection(Player player, Identifier hazard) {
        return false;
    }

    @Override
    public void hurt(LivingEntity entity, ResourceKey<DamageType> type, float amount) {
        // TODO: HazardDamage.hurt(entity, type, amount) when Workshop Core 0.5.0 is available.
    }

    @Override
    public void setHaze(ServerPlayer player, int argb, float density, int fadeTicks) {
        // TODO: Haze.set(player, argb, density, fadeTicks) when Workshop Core 0.5.0 is available.
    }

    @Override
    public void clearHaze(ServerPlayer player, int fadeTicks) {
        // TODO: Haze.clear(player, fadeTicks) when Workshop Core 0.5.0 is available.
    }
}
