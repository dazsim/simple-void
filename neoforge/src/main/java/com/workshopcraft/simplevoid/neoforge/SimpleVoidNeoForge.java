package com.workshopcraft.simplevoid.neoforge;

import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.command.VoidCommands;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(SimpleVoid.MOD_ID)
public final class SimpleVoidNeoForge {
    public SimpleVoidNeoForge() {
        SimpleVoid.initialize(FMLPaths.CONFIGDIR.get(), id -> ModList.get().isLoaded(id));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> VoidCommands.register(event.getDispatcher()));
    }
}
