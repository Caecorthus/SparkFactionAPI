package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FactionEconomyPolicy {
    @Nullable
    Boolean shouldReceiveReward(PlayerEntity player, RewardKind rewardKind, GameWorldComponent gameComponent);

    enum RewardKind {
        PASSIVE,
        DIRECT_KILL,
        TEAM_KILL,
        TASK
    }
}
