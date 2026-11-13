package com.workshopcraft.simplevoid.api;

import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.area.AreaHazard;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class VoidAreaEvents {
    private static final CopyOnWriteArrayList<Consumer<StateChange>> LISTENERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Consumer<HazardPresence>> HAZARD_LISTENERS = new CopyOnWriteArrayList<>();
    private VoidAreaEvents() {}

    public static Subscription listen(Consumer<StateChange> listener) {
        Objects.requireNonNull(listener);
        Consumer<StateChange> registration = listener::accept;
        LISTENERS.add(registration);
        return () -> LISTENERS.remove(registration);
    }

    public static Subscription listenHazards(Consumer<HazardPresence> listener) {
        Objects.requireNonNull(listener);
        Consumer<HazardPresence> registration = listener::accept;
        HAZARD_LISTENERS.add(registration);
        return () -> HAZARD_LISTENERS.remove(registration);
    }

    public static void post(StateChange change) {
        for (var listener : LISTENERS) {
            try {
                listener.accept(change);
            } catch (RuntimeException exception) {
                LogUtils.getLogger().error("Simple Void state-change listener failed for {}", change.after().id(), exception);
            }
        }
    }

    public static void post(HazardPresence change) {
        for (var listener : HAZARD_LISTENERS) {
            try {
                listener.accept(change);
            } catch (RuntimeException exception) {
                LogUtils.getLogger().error("Simple Void hazard-presence listener failed", exception);
            }
        }
    }

    public record StateChange(ServerLevel level, VoidArea before, VoidArea after) {}
    public record HazardPresence(ServerLevel level, ServerPlayer player, List<AreaHazard> previous, List<AreaHazard> current) {
        public HazardPresence {
            previous = List.copyOf(previous);
            current = List.copyOf(current);
        }
    }
    public interface Subscription extends AutoCloseable { @Override void close(); }
}
