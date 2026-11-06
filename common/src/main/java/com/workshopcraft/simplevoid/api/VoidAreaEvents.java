package com.workshopcraft.simplevoid.api;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;

public final class VoidAreaEvents {
    private static final CopyOnWriteArrayList<Consumer<StateChange>> LISTENERS = new CopyOnWriteArrayList<>();
    private VoidAreaEvents() {}

    public static Subscription listen(Consumer<StateChange> listener) {
        Objects.requireNonNull(listener);
        Consumer<StateChange> registration = listener::accept;
        LISTENERS.add(registration);
        return () -> LISTENERS.remove(registration);
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

    public record StateChange(ServerLevel level, VoidArea before, VoidArea after) {}
    public interface Subscription extends AutoCloseable { @Override void close(); }
}
