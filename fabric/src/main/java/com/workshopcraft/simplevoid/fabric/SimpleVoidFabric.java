package com.workshopcraft.simplevoid.fabric;

import com.workshopcraft.simplevoid.SimpleVoid;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class SimpleVoidFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleVoid.initialize(FabricLoader.getInstance().getConfigDir());
    }
}
