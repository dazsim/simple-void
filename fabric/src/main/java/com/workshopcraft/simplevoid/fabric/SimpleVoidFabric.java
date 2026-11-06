package com.workshopcraft.simplevoid.fabric;

import com.workshopcraft.simplevoid.SimpleVoid;
import com.workshopcraft.simplevoid.command.VoidCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class SimpleVoidFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleVoid.initialize(FabricLoader.getInstance().getConfigDir(), FabricLoader.getInstance()::isModLoaded);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> VoidCommands.register(dispatcher));
    }
}
