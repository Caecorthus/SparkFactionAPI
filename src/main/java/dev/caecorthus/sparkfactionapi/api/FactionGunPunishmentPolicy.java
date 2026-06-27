package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FactionGunPunishmentPolicy {
    /**
     * Returns whether this player is punishable as an innocent gun subject, or null to keep default behavior.
     * 返回该玩家是否按无辜枪击主体受罚；返回 null 表示保留默认行为。
     */
    @Nullable
    Boolean isPunishable(PlayerEntity player, Subject subject, GameWorldComponent gameComponent);

    enum Subject {
        VICTIM,
        SHOOTER
    }
}
