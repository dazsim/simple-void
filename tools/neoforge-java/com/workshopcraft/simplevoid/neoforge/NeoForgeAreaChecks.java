package com.workshopcraft.simplevoid.neoforge;

import com.workshopcraft.simplevoid.api.VoidArea;
import com.workshopcraft.simplevoid.api.VoidAreaEvents;
import com.workshopcraft.simplevoid.area.AreaBounds;
import com.workshopcraft.simplevoid.area.AreaHooks;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeAreaChecks {
    public static void main(String[] args) {
        try {
            var area = new VoidArea(Identifier.parse("homebound:colony/ship/bridge"), "bridge", Identifier.parse("homebound:colony/ship"),
                    new BlockPos(10,64,30), Rotation.CLOCKWISE_90, new AreaBounds(new BlockPos(3,65,31), new BlockPos(9,67,37)), "damaged", AreaHooks.NONE);
            var after = area.withState("repaired");
            AtomicInteger received = new AtomicInteger();
            NeoForge.EVENT_BUS.addListener(AreaStateChangedEvent.class, event -> {
                if (event.getLevel() != null || !event.getBefore().equals(area) || !event.getAfter().equals(after)) {
                    throw new AssertionError("NeoForge area event changed its payload");
                }
                received.incrementAndGet();
            });
            NeoForge.EVENT_BUS.start();
            var bridge = SimpleVoidNeoForge.forwardAreaChanges();
            VoidAreaEvents.post(new VoidAreaEvents.StateChange(null, area, after));
            if (received.get() != 1) throw new AssertionError("Common event did not reach NeoForge.EVENT_BUS");
            bridge.close();
            VoidAreaEvents.post(new VoidAreaEvents.StateChange(null, area, after));
            if (received.get() != 1) throw new AssertionError("Closed forwarding subscription still posted events");
            System.out.println("Passed 3 NeoForge area-event bridge checks. No server or client started.");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
