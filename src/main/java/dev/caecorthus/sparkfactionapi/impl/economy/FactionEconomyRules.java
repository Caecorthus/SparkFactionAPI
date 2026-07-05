package dev.caecorthus.sparkfactionapi.impl.economy;

import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class FactionEconomyRules {
    private FactionEconomyRules() {
    }

    public static boolean receivesKillerPassiveMoney(Role role) {
        return FactionCapabilityLookup.capabilities(role).receivesKillerPassiveMoney();
    }

    public static boolean receivesKillerPassiveMoney(PlayerEntity player, GameWorldComponent gameComponent) {
        return economyDecision(
                player,
                FactionEconomyPolicy.RewardKind.PASSIVE,
                gameComponent,
                FactionCapabilityLookup.capabilities(player, gameComponent).receivesKillerPassiveMoney()
        );
    }

    /**
     * Prevents passive-money add-ons from double-paying native or SparkTraits-adjusted killers.
     * 防止追加被动金钱时重复支付原生杀手或 SparkTraits 已调整过的杀手。
     */
    public static boolean receivesCustomKillerPassiveMoney(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)
                && !gameComponent.canUseKillerFeatures(player)
                && receivesKillerPassiveMoney(player, gameComponent);
    }

    public static boolean receivesKillReward(Role role) {
        return FactionCapabilityLookup.capabilities(role).receivesKillRewards();
    }

    public static boolean receivesKillReward(PlayerEntity player, GameWorldComponent gameComponent) {
        return economyDecision(
                player,
                FactionEconomyPolicy.RewardKind.DIRECT_KILL,
                gameComponent,
                FactionCapabilityLookup.capabilities(player, gameComponent).receivesKillRewards()
        );
    }

    /**
     * Restricts direct kill reward compensation to custom factions skipped by Wathe's native path.
     * 将直接击杀奖励补偿限制在 wathe 原生路径跳过的自定义阵营上。
     */
    public static boolean receivesCustomKillReward(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)
                && !gameComponent.canUseKillerFeatures(player)
                && receivesKillReward(player, gameComponent);
    }

    private static boolean economyDecision(
            PlayerEntity player,
            FactionEconomyPolicy.RewardKind rewardKind,
            GameWorldComponent gameComponent,
            boolean fallback
    ) {
        Boolean policyResult = firstEconomyPolicyResult(player, rewardKind, gameComponent);
        return policyResult == null ? fallback : policyResult;
    }

    private static @Nullable Boolean firstEconomyPolicyResult(
            PlayerEntity player,
            FactionEconomyPolicy.RewardKind rewardKind,
            GameWorldComponent gameComponent
    ) {
        for (FactionEconomyPolicy policy : FactionEconomyPolicies.economyPolicies()) {
            Boolean result = policy.shouldReceiveReward(player, rewardKind, gameComponent);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
