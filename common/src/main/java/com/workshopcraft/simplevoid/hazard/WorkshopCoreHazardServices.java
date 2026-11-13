package com.workshopcraft.simplevoid.hazard;

import com.workshopcraft.workshopcore.hazard.HazardDamage;
import com.workshopcraft.workshopcore.hazard.HazardProtection;
import com.workshopcraft.workshopcore.hazard.Haze;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class WorkshopCoreHazardServices implements HazardServices {
    @Override
    public boolean isProtected(Player player, Identifier hazard) {
        return HazardProtection.isProtected(player, hazard);
    }

    @Override
    public boolean tickProtection(Player player, Identifier hazard) {
        return HazardProtection.tick(player, hazard);
    }

    @Override
    public void hurt(LivingEntity entity, ResourceKey<DamageType> type, float amount) {
        HazardDamage.hurt(entity, type, amount);
    }

    @Override
    public void setHaze(ServerPlayer player, int argb, float density, int fadeTicks) {
        Haze.set(player, argb, density, fadeTicks);
    }

    @Override
    public void clearHaze(ServerPlayer player, int fadeTicks) {
        Haze.clear(player, fadeTicks);
    }
}
