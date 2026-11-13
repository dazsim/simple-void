package com.workshopcraft.simplevoid.neoforge;

import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import com.workshopcraft.simplevoid.area.AreaEngine;
import com.workshopcraft.simplevoid.backdrop.BackdropHookEngine;
import com.workshopcraft.simplevoid.command.VoidCommands;
import com.workshopcraft.simplevoid.hazard.HazardCatalog;
import com.workshopcraft.simplevoid.hazard.HazardEngine;
import com.workshopcraft.simplevoid.hazard.VoidConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@Mod(SimpleVoid.MOD_ID)
public final class SimpleVoidNeoForge {
    public SimpleVoidNeoForge() {
        SimpleVoid.initialize(FMLPaths.CONFIGDIR.get(), id -> ModList.get().isLoaded(id));
        VoidConfig.reload();
        BackdropHookEngine.initialize();
        forwardAreaChanges();
        forwardHazardChanges();
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            VoidConfig.reload();
            HazardCatalog.invalidate();
            event.getServer().getAllLevels().forEach(AreaEngine::initialize);
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> VoidCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) HazardEngine.tick(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) HazardEngine.refresh(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) HazardEngine.refresh(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) HazardEngine.refresh(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> HazardEngine.forget(event.getEntity().getUUID()));
        VoidAreaEvents.listen(change -> {
            if (change.level() == null) return;
            for (ServerPlayer player : change.level().players()) HazardEngine.refresh(player);
        });
    }

    static VoidAreaEvents.Subscription forwardAreaChanges() {
        return VoidAreaEvents.listen(change -> NeoForge.EVENT_BUS.post(new AreaStateChangedEvent(change)));
    }

    static VoidAreaEvents.Subscription forwardHazardChanges() {
        return VoidAreaEvents.listenHazards(change -> NeoForge.EVENT_BUS.post(new PlayerHazardsChangedEvent(change)));
    }
}
