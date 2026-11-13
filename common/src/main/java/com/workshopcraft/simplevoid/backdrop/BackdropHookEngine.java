package com.workshopcraft.simplevoid.backdrop;

import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.start.StartConfig;
import com.workshopcraft.simplevoid.start.StartState;
import com.workshopcraft.simplevoid.start.VoidStart;
import com.workshopcraft.workshopcore.backdrop.BackdropEvents;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class BackdropHookEngine {
    private static boolean registered;

    private BackdropHookEngine() {}

    public static synchronized void initialize() {
        if (registered) return;
        BackdropEvents.listen(BackdropHookEngine::onChange);
        registered = true;
    }

    static void onChange(ServerLevel level, Optional<Identifier> oldId, Optional<Identifier> newId) {
        if (level == null || !VoidStart.isVoid(level)) return;
        var selected = StartState.get(level).selectedSetup();
        if (selected.isEmpty()) return;
        try {
            var setup = StartConfig.load(level.getServer().getResourceManager(), selected.get());
            Identifier dimension = level.dimension().identifier();
            for (String command : setup.onBackdropChange().forBackdrop(newId)) {
                BackdropCommandHooks.run(level, BackdropHooks.expand(command, newId, oldId, dimension));
            }
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Simple Void backdrop hook failed for {}", selected.get(), exception);
        }
    }
}
