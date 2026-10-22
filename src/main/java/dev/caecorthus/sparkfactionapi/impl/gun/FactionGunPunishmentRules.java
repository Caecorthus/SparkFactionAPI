package dev.caecorthus.sparkfactionapi.impl.gun;

import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class FactionGunPunishmentRules {
    private FactionGunPunishmentRules() {
    }

    /**
     * Marks custom-faction victims that should trigger innocent-shot punishment additively.
     * 标记需要追加触发射击无辜惩罚的自定义阵营受击者。
     */
    public static boolean isCustomPunishableInnocentGunVictim(PlayerEntity player, GameWorldComponent gameComponent) {
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(player, gameComponent);
        return FactionCapabilityLookup.isCustomFaction(factionId)
                && !gameComponent.isInnocent(player)
                && victimDecision(player, gameComponent, factionId);
    }

    public static @Nullable Boolean gunPunishmentOverride(
            PlayerEntity player,
            FactionGunPunishmentPolicy.Subject subject,
            GameWorldComponent gameComponent
    ) {
        return firstGunPunishmentPolicyResult(player, subject, gameComponent);
    }

    public static boolean isPunishableInnocentGunShooter(PlayerEntity player, GameWorldComponent gameComponent) {
        return shooterDecision(player, gameComponent, FactionCapabilityLookup.effectiveFaction(player, gameComponent));
    }

    /**
     * Custom punishable shooters spend revolvers like Wathe killers without entering the native killer bucket.
     * 自定义阵营的可惩罚开枪者像 wathe 杀手一样消耗左轮，但不进入原生杀手阵营桶。
     */
    public static boolean consumesPunishableGunLikeKiller(PlayerEntity player, GameWorldComponent gameComponent) {
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(player, gameComponent);
        return canUseKillerFeatureAccess(factionId)
                || (FactionCapabilityLookup.isCustomFaction(factionId)
                && shooterDecision(player, gameComponent, factionId));
    }

    static boolean usesCustomShooterDecision(
            PlayerEntity shooter,
            @Nullable Boolean shooterOverride,
            GameWorldComponent gameComponent
    ) {
        return shooterOverride != null || FactionCapabilityLookup.hasCustomEffectiveFaction(shooter, gameComponent);
    }

    private static boolean canUseKillerFeatureAccess(Identifier effectiveFaction) {
        return FactionCapabilityLookup.capabilitiesForFaction(effectiveFaction).canUseKillerFeatures();
    }

    private static boolean victimDecision(
            PlayerEntity player,
            GameWorldComponent gameComponent,
            Identifier effectiveFaction
    ) {
        return gunPunishmentDecision(
                player,
                FactionGunPunishmentPolicy.Subject.VICTIM,
                gameComponent,
                FactionCapabilityLookup.capabilitiesForFaction(effectiveFaction).isPunishableInnocentGunVictim()
        );
    }

    private static boolean shooterDecision(
            PlayerEntity player,
            GameWorldComponent gameComponent,
            Identifier effectiveFaction
    ) {
        return gunPunishmentDecision(
                player,
                FactionGunPunishmentPolicy.Subject.SHOOTER,
                gameComponent,
                FactionCapabilityLookup.capabilitiesForFaction(effectiveFaction).isPunishableInnocentGunShooter()
        );
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
