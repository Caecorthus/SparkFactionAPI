package dev.caecorthus.sparkfactionapi.impl.economy;

import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FactionEconomyRules {
    // Owns economy policy registration and ordering: registration order is evaluation order. Copy-on-write, so an
    // evaluation iterates a snapshot and a policy registered mid-pass neither throws nor joins that pass.
    // 持有经济策略的注册与顺序：注册顺序即评估顺序。写时复制，评估遍历快照，评估中途注册的策略既不抛异常也不加入本轮。
    private static final List<FactionEconomyPolicy> ECONOMY_POLICIES = new CopyOnWriteArrayList<>();

    private FactionEconomyRules() {
    }

    public static void register(FactionEconomyPolicy policy) {
        ECONOMY_POLICIES.add(policy);
    }

    /**
     * Prevents passive-money add-ons from double-paying native or SparkTraits-adjusted killers.
     * 防止追加被动金钱时重复支付原生杀手或 SparkTraits 已调整过的杀手。
     */
    public static boolean receivesCustomKillerPassiveMoney(PlayerEntity player, GameWorldComponent gameComponent) {
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(player, gameComponent);
        return FactionCapabilityLookup.isCustomFaction(factionId)
                && !gameComponent.canUseKillerFeatures(player)
                && passiveMoneyDecision(player, gameComponent, factionId);
    }

    /**
     * Restricts direct kill reward compensation to custom factions skipped by Wathe's native path.
     * 将直接击杀奖励补偿限制在 wathe 原生路径跳过的自定义阵营上。
     */
    public static boolean receivesCustomKillReward(PlayerEntity player, GameWorldComponent gameComponent) {
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(player, gameComponent);
        return FactionCapabilityLookup.isCustomFaction(factionId)
                && !gameComponent.canUseKillerFeatures(player)
                && killRewardDecision(player, gameComponent, factionId);
    }

    private static boolean passiveMoneyDecision(
            PlayerEntity player,
            GameWorldComponent gameComponent,
            Identifier effectiveFaction
    ) {
        return economyDecision(
                player,
                FactionEconomyPolicy.RewardKind.PASSIVE,
                gameComponent,
                FactionCapabilityLookup.capabilitiesForFaction(effectiveFaction).receivesKillerPassiveMoney()
        );
    }

    private static boolean killRewardDecision(
            PlayerEntity player,
            GameWorldComponent gameComponent,
            Identifier effectiveFaction
    ) {
        return economyDecision(
                player,
                FactionEconomyPolicy.RewardKind.DIRECT_KILL,
                gameComponent,
                FactionCapabilityLookup.capabilitiesForFaction(effectiveFaction).receivesKillRewards()
        );
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
        for (FactionEconomyPolicy policy : ECONOMY_POLICIES) {
            Boolean result = policy.shouldReceiveReward(player, rewardKind, gameComponent);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
