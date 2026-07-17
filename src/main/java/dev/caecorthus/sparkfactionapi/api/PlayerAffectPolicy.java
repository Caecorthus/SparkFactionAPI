package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Vetoes player-to-player effects. Every registered policy runs and any denial wins.
 * 否决玩家之间的影响；所有已注册策略都会执行，任一拒绝即为最终结果。
 */
@FunctionalInterface
public interface PlayerAffectPolicy {
    boolean canAffect(
            PlayerEntity actor,
            PlayerEntity target,
            Identifier actionId,
            GameWorldComponent gameComponent
    );
}
