package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FactionBlackoutCooldownPolicy {
    /**
     * Returns whether a blackout purchaser should share cooldown with a target, or null to keep default behavior.
     * 返回关灯购买者是否应与目标共享冷却；返回 null 表示保留默认行为。
     */
    @Nullable
    Boolean shouldShareCooldown(PlayerEntity purchaser, PlayerEntity target, GameWorldComponent gameComponent);
}
