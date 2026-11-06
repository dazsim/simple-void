package com.workshopcraft.simplevoid.neoforge;

import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import com.workshopcraft.simplevoid.area.AreaEngine;
import com.workshopcraft.simplevoid.command.VoidCommands;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(SimpleVoid.MOD_ID)
public final class SimpleVoidNeoForge {
    public SimpleVoidNeoForge() {
        SimpleVoid.initialize(FMLPaths.CONFIGDIR.get(), id -> ModList.get().isLoaded(id));
        forwardAreaChanges();
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> event.getServer().getAllLevels().forEach(AreaEngine::initialize));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> VoidCommands.register(event.getDispatcher()));
    }

    static VoidAreaEvents.Subscription forwardAreaChanges() {
        return VoidAreaEvents.listen(change -> NeoForge.EVENT_BUS.post(new AreaStateChangedEvent(change)));
    }
}
