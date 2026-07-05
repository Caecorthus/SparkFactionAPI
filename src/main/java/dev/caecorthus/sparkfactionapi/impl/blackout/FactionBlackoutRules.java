package dev.caecorthus.sparkfactionapi.impl.blackout;

import dev.caecorthus.sparkfactionapi.api.FactionBlackoutCooldownPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class FactionBlackoutRules {
    private FactionBlackoutRules() {
    }

    /**
     * Keeps blackout night vision compatible with older killer-feature capability users.
     * 让黑灯夜视兼容旧的杀手功能能力使用者。
     */
    public static boolean hasBlackoutImmunity(Role role) {
        FactionCapabilities capabilities = FactionCapabilityLookup.capabilities(role);
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
        FactionCapabilities capabilities = FactionCapabilityLookup.capabilities(player, gameComponent);
        return capabilities.hasBlackoutImmunity() || capabilities.canUseKillerFeatures();
    }

    public static @Nullable Boolean blackoutCooldownOverride(
            PlayerEntity purchaser,
            PlayerEntity target,
            GameWorldComponent gameComponent
    ) {
        return firstBlackoutCooldownPolicyResult(purchaser, target, gameComponent);
    }

    /**
     * Adds blackout cooldown sharing only for custom faction players skipped by Wathe's native killer path.
     * 只给 wathe 原生杀手路径跳过的自定义阵营玩家追加熄灯冷却共享。
     */
    public static boolean sharesCustomBlackoutCooldown(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)
                && !gameComponent.canUseKillerFeatures(player)
                && canUseKillerFeatureAccess(player, gameComponent);
    }

    private static boolean canUseKillerFeatureAccess(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.capabilities(player, gameComponent).canUseKillerFeatures();
    }

    private static @Nullable Boolean firstBlackoutCooldownPolicyResult(
            PlayerEntity purchaser,
            PlayerEntity target,
            GameWorldComponent gameComponent
    ) {
        for (FactionBlackoutCooldownPolicy policy : FactionBlackoutCooldownPolicies.blackoutCooldownPolicies()) {
            Boolean result = policy.shouldShareCooldown(purchaser, target, gameComponent);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
