package com.workshopcraft.simplevoid.area;

import com.mojang.logging.LogUtils;
import com.workshopcraft.simplevoid.api.VoidArea;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

public final class AreaTransitions {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private AreaTransitions() {}

    public static boolean set(AreaSavedData data, Identifier id, String state, Consumer<Change> listener,
                              BiConsumer<VoidArea, String> commands) {
        AreaDefinition.requireState(state);
        VoidArea before = data.require(id);
        if (before.state().equals(state)) return false;
        if (ACTIVE.get()) throw new IllegalStateException("Cannot change area state from a state-change listener");
        ACTIVE.set(true);
        VoidArea after;
        try {
            after = data.update(id, state);
            try {
                listener.accept(new Change(before, after));
            } catch (RuntimeException exception) {
                LogUtils.getLogger().error("Simple Void area listener failed for {}", id, exception);
            }
        } finally {
            ACTIVE.remove();
        }
        for (String command : after.onStateChange().forState(state)) {
            try {
                commands.accept(after, AreaHooks.expand(command, after, before.state()));
            } catch (RuntimeException exception) {
                LogUtils.getLogger().error("Simple Void area command failed for {}: {}", id, command, exception);
            }
        }
        return true;
    }

    public record Change(VoidArea before, VoidArea after) {}
}
