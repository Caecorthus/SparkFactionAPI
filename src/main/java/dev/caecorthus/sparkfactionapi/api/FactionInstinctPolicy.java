package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FactionInstinctPolicy {
    @Nullable
    InstinctResult getHighlight(PlayerEntity viewer, Entity target, GameWorldComponent gameComponent);

    record InstinctResult(int color, boolean requiresKeybind, int priority, boolean skip) {
        public static InstinctResult skip(int priority) {
            return new InstinctResult(-1, false, priority, true);
        }

        public static InstinctResult show(int color, boolean requiresKeybind, int priority) {
            return new InstinctResult(color, requiresKeybind, priority, false);
        }
    }
}
