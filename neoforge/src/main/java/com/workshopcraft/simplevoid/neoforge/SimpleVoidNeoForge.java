package com.workshopcraft.simplevoid.neoforge;

import com.workshopcraft.simplevoid.SimpleVoid;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(SimpleVoid.MOD_ID)
public final class SimpleVoidNeoForge {
    public SimpleVoidNeoForge() {
        SimpleVoid.initialize(FMLPaths.CONFIGDIR.get());
    }
}
