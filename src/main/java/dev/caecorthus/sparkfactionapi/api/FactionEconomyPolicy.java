package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FactionEconomyPolicy {
    @Nullable
    Boolean shouldReceiveReward(PlayerEntity player, RewardKind rewardKind, GameWorldComponent gameComponent);

    enum RewardKind {
        /** Passive income ticks. / 被动收入。 */
        PASSIVE,
        /** The reward for a kill the player made. / 玩家亲手击杀的奖励。 */
        DIRECT_KILL,
        /**
         * Reserved: SparkFactionAPI does not consult policies with this kind yet, so answering for it has no effect.
         * 预留：SparkFactionAPI 目前不会用该类型询问策略，对它作答不会产生效果。
         */
        TEAM_KILL,
        /**
         * Reserved: SparkFactionAPI does not consult policies with this kind yet, so answering for it has no effect.
         * 预留：SparkFactionAPI 目前不会用该类型询问策略，对它作答不会产生效果。
         */
        TASK
    }
}
