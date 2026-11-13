package com.workshopcraft.simplevoid.hazard;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Damage, protective-gear wear and haze come from Workshop Core 0.5.0
 * ({@code com.workshopcraft.workshopcore.hazard}). That version is not in the
 * local maven yet, so a stub is used until it is.
 */
public interface HazardServices {
    boolean isProtected(Player player, Identifier hazard);

    boolean tickProtection(Player player, Identifier hazard);

    void hurt(LivingEntity entity, ResourceKey<DamageType> type, float amount);

    void setHaze(ServerPlayer player, int argb, float density, int fadeTicks);

    void clearHaze(ServerPlayer player, int fadeTicks);

    static HazardServices get() {
        return Holder.CURRENT;
    }

    static void set(HazardServices services) {
        Holder.CURRENT = services;
    }

    final class Holder {
        private static HazardServices CURRENT = new StubHazardServices();

        private Holder() {}
    }
}
