package com.workshopcraft.simplevoid.hazard;

import com.workshopcraft.simplevoid.api.VoidArea;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import com.workshopcraft.simplevoid.area.AreaBounds;
import com.workshopcraft.simplevoid.area.AreaEngine;
import com.workshopcraft.simplevoid.area.AreaHazard;
import com.workshopcraft.simplevoid.area.AreaHazards;
import com.workshopcraft.simplevoid.area.HazeSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public final class HazardEngine {
    private static final Map<UUID, List<AreaHazard>> LAST = new LinkedHashMap<>();

    private HazardEngine() {}

    public static boolean intersects(AreaBounds bounds, AABB box) {
        return bounds.intersects(box);
    }

    public static List<VoidArea> containing(List<VoidArea> areas, AABB box) {
        return areas.stream().filter(area -> area.bounds().intersects(box)).toList();
    }

    public static List<AreaHazard> merge(List<AreaHazard> hazards) {
        var merged = new LinkedHashMap<Identifier, AreaHazard>();
        for (AreaHazard hazard : hazards) {
            merged.merge(hazard.type(), hazard, AreaHazard::harsher);
        }
        return List.copyOf(merged.values());
    }

    public static List<AreaHazard> resolve(List<VoidArea> inside, Function<VoidArea, AreaHazards> definitions) {
        List<AreaHazard> hazards = new ArrayList<>();
        for (VoidArea area : inside) {
            hazards.addAll(definitions.apply(area).forState(area.state()));
        }
        return merge(hazards);
    }

    public static boolean shouldApplyEffects(boolean hazardsEnabled, boolean creative, boolean spectator) {
        return hazardsEnabled && !creative && !spectator;
    }

    public static boolean shouldShowHaze(boolean hazardsEnabled, boolean hazeEnabled, boolean spectator) {
        return hazardsEnabled && hazeEnabled && !spectator;
    }

    public static HazeSpec overlay(List<AreaHazard> hazards) {
        HazeSpec chosen = null;
        for (AreaHazard hazard : hazards) {
            HazeSpec candidate = hazard.hazeOrDefault();
            if (chosen == null || candidate.density() > chosen.density()) chosen = candidate;
        }
        return chosen;
    }

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        List<AreaHazard> previous = LAST.getOrDefault(player.getUUID(), List.of());
        if (!VoidConfig.hazardsEnabled() || player.isSpectator()) {
            publish(level, player, previous, List.of());
            if (VoidConfig.hazeEnabled()) HazardServices.get().clearHaze(player, 20);
            return;
        }
        var areas = AreaEngine.initialize(level).list();
        List<AreaHazard> active = resolve(containing(areas, player.getBoundingBox()), area -> HazardCatalog.defined(level, area));
        if (shouldApplyEffects(true, player.isCreative(), false)) {
            apply(player, active);
        }
        HazeSpec haze = shouldShowHaze(true, VoidConfig.hazeEnabled(), false) ? overlay(active) : null;
        var services = HazardServices.get();
        if (haze == null) services.clearHaze(player, active.isEmpty() ? 20 : 20);
        else services.setHaze(player, haze.color(), haze.density(), haze.fadeTicks());
        publish(level, player, previous, active);
    }

    public static void refresh(ServerPlayer player) {
        LAST.remove(player.getUUID());
        tick(player);
    }

    public static void forget(UUID id) {
        LAST.remove(id);
    }

    private static void apply(ServerPlayer player, List<AreaHazard> hazards) {
        var services = HazardServices.get();
        for (AreaHazard hazard : hazards) {
            if (services.tickProtection(player, hazard.type())) continue;
            if (hazard.airPerTick() > 0) {
                player.setAirSupply(Math.max(-20, player.getAirSupply() - hazard.airPerTick()));
            }
            if (hazard.freezePerTick() > 0) {
                player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 40, player.getTicksFrozen() + hazard.freezePerTick()));
            }
            boolean due = (player.tickCount + player.getId()) % hazard.intervalTicks() == 0 && hazard.damage() > 0;
            boolean noAirReady = !hazard.type().equals(AreaHazard.NO_AIR) || player.getAirSupply() <= 0;
            if (due && noAirReady) {
                services.hurt(player, ResourceKey.create(Registries.DAMAGE_TYPE, hazard.damageType()), hazard.damage());
            }
        }
    }

    private static void publish(ServerLevel level, ServerPlayer player, List<AreaHazard> previous, List<AreaHazard> current) {
        if (previous.equals(current)) {
            LAST.put(player.getUUID(), current);
            return;
        }
        LAST.put(player.getUUID(), current);
        VoidAreaEvents.post(new VoidAreaEvents.HazardPresence(level, player, previous, current));
    }
}
