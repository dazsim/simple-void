package com.workshopcraft.simplevoid.neoforge;

import com.workshopcraft.simplevoid.area.AreaHazard;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class PlayerHazardsChangedEvent extends Event {
    private final VoidAreaEvents.HazardPresence change;

    public PlayerHazardsChangedEvent(VoidAreaEvents.HazardPresence change) { this.change = change; }
    public ServerLevel getLevel() { return change.level(); }
    public ServerPlayer getPlayer() { return change.player(); }
    public List<AreaHazard> getPrevious() { return change.previous(); }
    public List<AreaHazard> getCurrent() { return change.current(); }
}
