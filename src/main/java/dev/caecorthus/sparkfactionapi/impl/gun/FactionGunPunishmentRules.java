package dev.caecorthus.sparkfactionapi.impl.gun;

import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class FactionGunPunishmentRules {
    private FactionGunPunishmentRules() {
    }

    public static boolean isPunishableInnocentGunVictim(Role role) {
        return FactionCapabilityLookup.capabilities(role).isPunishableInnocentGunVictim();
    }

    public static boolean isPunishableInnocentGunVictim(PlayerEntity player, GameWorldComponent gameComponent) {
        return gunPunishmentDecision(
                player,
                FactionGunPunishmentPolicy.Subject.VICTIM,
                gameComponent,
                FactionCapabilityLookup.capabilities(player, gameComponent).isPunishableInnocentGunVictim()
        );
    }

    /**
     * Marks custom-faction victims that should trigger innocent-shot punishment additively.
     * 标记需要追加触发射击无辜惩罚的自定义阵营受击者。
     */
    public static boolean isCustomPunishableInnocentGunVictim(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)
                && !gameComponent.isInnocent(player)
                && isPunishableInnocentGunVictim(player, gameComponent);
    }

    public static @Nullable Boolean gunPunishmentOverride(
            PlayerEntity player,
            FactionGunPunishmentPolicy.Subject subject,
            GameWorldComponent gameComponent
    ) {
        return firstGunPunishmentPolicyResult(player, subject, gameComponent);
    }

    public static boolean isPunishableInnocentGunShooter(Role role) {
        return FactionCapabilityLookup.capabilities(role).isPunishableInnocentGunShooter();
    }

    public static boolean isPunishableInnocentGunShooter(PlayerEntity player, GameWorldComponent gameComponent) {
        return gunPunishmentDecision(
                player,
                FactionGunPunishmentPolicy.Subject.SHOOTER,
                gameComponent,
                FactionCapabilityLookup.capabilities(player, gameComponent).isPunishableInnocentGunShooter()
        );
    }

    /**
     * Marks custom-faction shooters whose gun punishment should be handled outside native checks.
     * 标记需要在原生判断外处理枪罚的自定义阵营开枪者。
     */
    public static boolean isCustomPunishableInnocentGunShooter(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)
                && !gameComponent.isInnocent(player)
                && isPunishableInnocentGunShooter(player, gameComponent);
    }

    /**
     * Custom punishable shooters spend revolvers like Wathe killers without entering the native killer bucket.
     * 自定义阵营的可惩罚开枪者像 wathe 杀手一样消耗左轮，但不进入原生杀手阵营桶。
     */
    public static boolean consumesPunishableGunLikeKiller(PlayerEntity player, GameWorldComponent gameComponent) {
        return canUseKillerFeatureAccess(player, gameComponent)
                || (FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)
                && isPunishableInnocentGunShooter(player, gameComponent));
    }

    public static boolean consumesPunishableGunLikeKiller(Role role) {
        return canUseKillerFeatureAccess(role)
                || (FactionCapabilityLookup.hasCustomBaseFaction(role) && isPunishableInnocentGunShooter(role));
    }

    static boolean usesCustomShooterDecision(
            PlayerEntity shooter,
            @Nullable Boolean shooterOverride,
            GameWorldComponent gameComponent
    ) {
        return shooterOverride != null || FactionCapabilityLookup.hasCustomEffectiveFaction(shooter, gameComponent);
    }

    private static boolean canUseKillerFeatureAccess(Role role) {
        return FactionCapabilityLookup.capabilities(role).canUseKillerFeatures();
    }

    private static boolean canUseKillerFeatureAccess(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.capabilities(player, gameComponent).canUseKillerFeatures();
    }

    private static boolean gunPunishmentDecision(
            PlayerEntity player,
            FactionGunPunishmentPolicy.Subject subject,
            GameWorldComponent gameComponent,
            boolean fallback
    ) {
        Boolean policyResult = firstGunPunishmentPolicyResult(player, subject, gameComponent);
        return policyResult == null ? fallback : policyResult;
    }

    private static @Nullable Boolean firstGunPunishmentPolicyResult(
            PlayerEntity player,
            FactionGunPunishmentPolicy.Subject subject,
            GameWorldComponent gameComponent
    ) {
        for (FactionGunPunishmentPolicy policy : FactionGunPunishmentPolicies.gunPunishmentPolicies()) {
            Boolean result = policy.isPunishable(player, subject, gameComponent);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
