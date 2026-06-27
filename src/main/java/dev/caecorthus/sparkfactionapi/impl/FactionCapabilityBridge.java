package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionTargetEligibility;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Purpose-specific bridge from Wathe call sites into SparkFactionAPI capabilities.
 * 从 wathe 调用点进入 SparkFactionAPI 能力系统的用途专用桥接层。
 */
public final class FactionCapabilityBridge {
    private FactionCapabilityBridge() {
    }

    public static boolean canUseKillerFeatureAccess(Role role) {
        return capabilities(role).canUseKillerFeatures();
    }

    public static boolean canUseKillerFeatureAccess(PlayerEntity player, GameWorldComponent gameComponent) {
        return capabilities(player, gameComponent).canUseKillerFeatures();
    }

    public static boolean receivesKillerPassiveMoney(Role role) {
        return capabilities(role).receivesKillerPassiveMoney();
    }

    public static boolean receivesKillerPassiveMoney(PlayerEntity player, GameWorldComponent gameComponent) {
        return economyDecision(
                player,
                FactionEconomyPolicy.RewardKind.PASSIVE,
                gameComponent,
                capabilities(player, gameComponent).receivesKillerPassiveMoney()
        );
    }

    public static boolean receivesKillReward(Role role) {
        return capabilities(role).receivesKillRewards();
    }

    public static boolean receivesKillReward(PlayerEntity player, GameWorldComponent gameComponent) {
        return economyDecision(
                player,
                FactionEconomyPolicy.RewardKind.DIRECT_KILL,
                gameComponent,
                capabilities(player, gameComponent).receivesKillRewards()
        );
    }

    public static boolean isPunishableInnocentGunVictim(Role role) {
        return capabilities(role).isPunishableInnocentGunVictim();
    }

    public static boolean isPunishableInnocentGunVictim(PlayerEntity player, GameWorldComponent gameComponent) {
        return capabilities(player, gameComponent).isPunishableInnocentGunVictim();
    }

    public static boolean isPunishableInnocentGunShooter(Role role) {
        return capabilities(role).isPunishableInnocentGunShooter();
    }

    public static boolean isPunishableInnocentGunShooter(PlayerEntity player, GameWorldComponent gameComponent) {
        return capabilities(player, gameComponent).isPunishableInnocentGunShooter();
    }

    /**
     * Keeps blackout night vision compatible with older killer-feature capability users.
     * 让黑灯夜视兼容旧的杀手功能能力使用者。
     */
    public static boolean hasBlackoutImmunity(Role role) {
        FactionCapabilities capabilities = capabilities(role);
        return capabilities.hasBlackoutImmunity() || capabilities.canUseKillerFeatures();
    }

    /**
     * Resolves per-player blackout night vision without granting shop, economy, cohort, or instinct access.
     * 按玩家解析黑灯夜视，不额外授予商店、经济、同伙或直觉能力。
     */
    public static boolean hasBlackoutImmunity(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null || gameComponent == null) {
            return false;
        }
        FactionCapabilities capabilities = capabilities(player, gameComponent);
        return capabilities.hasBlackoutImmunity() || capabilities.canUseKillerFeatures();
    }

    public static boolean sharesCohort(Role viewerRole, Role targetRole) {
        Identifier viewerFaction = FactionRegistryImpl.resolveBaseFaction(viewerRole);
        Identifier targetFaction = FactionRegistryImpl.resolveBaseFaction(targetRole);
        return viewerFaction.equals(targetFaction)
                && capabilities(viewerRole).sharesCohort()
                && capabilities(targetRole).sharesCohort();
    }

    public static boolean sharesCohort(
            PlayerEntity viewer,
            PlayerEntity target,
            GameWorldComponent gameComponent
    ) {
        Identifier viewerFaction = FactionRegistryImpl.resolveEffectiveFaction(viewer, gameComponent);
        Identifier targetFaction = FactionRegistryImpl.resolveEffectiveFaction(target, gameComponent);
        return viewerFaction.equals(targetFaction)
                && capabilities(viewer, gameComponent).sharesCohort()
                && capabilities(target, gameComponent).sharesCohort();
    }

    public static boolean canUseInstinct(Role role) {
        return capabilities(role).canUseInstinct();
    }

    public static boolean canUseInstinct(PlayerEntity player, GameWorldComponent gameComponent) {
        return capabilities(player, gameComponent).canUseInstinct();
    }

    public static int instinctColor(Role role) {
        return capabilities(role).instinctColor();
    }

    public static int instinctColor(PlayerEntity player, GameWorldComponent gameComponent) {
        return capabilities(player, gameComponent).instinctColor();
    }

    public static int instinctDisplayColor(Role role) {
        FactionCapabilities capabilities = capabilities(role);
        if (capabilities.instinctColor() != -1) {
            return capabilities.instinctColor();
        }
        Identifier factionId = FactionRegistryImpl.resolveBaseFaction(role);
        return FactionRegistryImpl.getFaction(factionId)
                .map(FactionDefinition::color)
                .orElse(-1);
    }

    public static int instinctDisplayColor(PlayerEntity player, GameWorldComponent gameComponent) {
        FactionCapabilities capabilities = capabilities(player, gameComponent);
        if (capabilities.instinctColor() != -1) {
            return capabilities.instinctColor();
        }
        Identifier factionId = FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent);
        return FactionRegistryImpl.getFaction(factionId)
                .map(FactionDefinition::color)
                .orElse(-1);
    }

    public static boolean hasCustomEffectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null || gameComponent == null) {
            return false;
        }
        return FactionRegistryImpl.isCustomFaction(FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent));
    }

    public static boolean canTarget(Role viewerRole, Role targetRole, Identifier targetTag) {
        return targetTag != null && capabilities(targetRole).targetTags().contains(targetTag);
    }

    public static boolean canTarget(
            PlayerEntity viewer,
            PlayerEntity target,
            Identifier targetTag,
            GameWorldComponent gameComponent
    ) {
        if (viewer == null || target == null || targetTag == null || gameComponent == null) {
            return false;
        }
        Boolean policyResult = firstTargetEligibilityResult(viewer, target, targetTag, gameComponent);
        return policyResult == null ? capabilities(target, gameComponent).targetTags().contains(targetTag) : policyResult;
    }

    public static Optional<FactionInstinctPolicy.InstinctResult> instinctPolicyResult(
            PlayerEntity viewer,
            Entity target,
            GameWorldComponent gameComponent
    ) {
        FactionInstinctPolicy.InstinctResult bestResult = null;
        for (FactionInstinctPolicy policy : FactionRegistryImpl.instinctPolicies()) {
            FactionInstinctPolicy.InstinctResult result = policy.getHighlight(viewer, target, gameComponent);
            if (result != null && (bestResult == null || result.priority() > bestResult.priority())) {
                bestResult = result;
            }
        }
        return Optional.ofNullable(bestResult);
    }

    public static FactionCapabilities capabilities(Role role) {
        return FactionRegistryImpl.capabilities(FactionRegistryImpl.resolveBaseFaction(role));
    }

    public static FactionCapabilities capabilities(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null || gameComponent == null) {
            return FactionCapabilities.none();
        }
        return FactionRegistryImpl.capabilities(FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent));
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
        for (FactionEconomyPolicy policy : FactionRegistryImpl.economyPolicies()) {
            Boolean result = policy.shouldReceiveReward(player, rewardKind, gameComponent);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static @Nullable Boolean firstTargetEligibilityResult(
            PlayerEntity viewer,
            PlayerEntity target,
            Identifier targetTag,
            GameWorldComponent gameComponent
    ) {
        for (FactionTargetEligibility eligibility : FactionRegistryImpl.targetEligibility()) {
            Boolean result = eligibility.canTarget(viewer, target, targetTag, gameComponent);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
