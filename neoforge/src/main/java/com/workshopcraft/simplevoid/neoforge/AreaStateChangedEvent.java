package com.workshopcraft.simplevoid.neoforge;

import com.workshopcraft.simplevoid.api.VoidArea;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public final class AreaStateChangedEvent extends Event {
    private final VoidAreaEvents.StateChange change;

    public AreaStateChangedEvent(VoidAreaEvents.StateChange change) { this.change = change; }
    public ServerLevel getLevel() { return change.level(); }
    public VoidArea getBefore() { return change.before(); }
    public VoidArea getAfter() { return change.after(); }
}
