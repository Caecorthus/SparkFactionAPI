package dev.caecorthus.sparkfactionapi.impl.vision;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class FactionInstinctRules {
    private FactionInstinctRules() {
    }

    public static boolean canUseInstinct(Role role) {
        return FactionCapabilityLookup.capabilities(role).canUseInstinct();
    }

    public static boolean canUseInstinct(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.capabilities(player, gameComponent).canUseInstinct();
    }

    /**
     * Dead spectators must fall through to Wathe's built-in spectator information colors.
     * 死亡旁观者必须回落到 wathe 原生旁观信息颜色，避免自定义阵营透视覆盖。
     */
    public static boolean shouldUseCustomHighlight(boolean viewerPlayingAndAlive, boolean viewerSpectatingOrCreative) {
        return viewerPlayingAndAlive || !viewerSpectatingOrCreative;
    }

    public static int instinctColor(Role role) {
        return FactionCapabilityLookup.capabilities(role).instinctColor();
    }

    public static int instinctColor(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.capabilities(player, gameComponent).instinctColor();
    }

    public static int displayColor(Role role) {
        FactionCapabilities capabilities = FactionCapabilityLookup.capabilities(role);
        if (capabilities.instinctColor() != -1) {
            return capabilities.instinctColor();
        }
        Identifier factionId = FactionCapabilityLookup.baseFaction(role);
        return FactionRegistryImpl.getFaction(factionId)
                .map(FactionDefinition::color)
                .orElse(-1);
    }

    public static int displayColor(PlayerEntity player, GameWorldComponent gameComponent) {
        FactionCapabilities capabilities = FactionCapabilityLookup.capabilities(player, gameComponent);
        if (capabilities.instinctColor() != -1) {
            return capabilities.instinctColor();
        }
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(player, gameComponent);
        return FactionRegistryImpl.getFaction(factionId)
                .map(FactionDefinition::color)
                .orElse(-1);
    }

    public static boolean hasCustomInstinctViewer(PlayerEntity viewer, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.hasCustomEffectiveFaction(viewer, gameComponent)
                && canUseInstinct(viewer, gameComponent);
    }

    public static Optional<FactionInstinctPolicy.InstinctResult> policyResult(
            PlayerEntity viewer,
            Entity target,
            GameWorldComponent gameComponent
    ) {
        FactionInstinctPolicy.InstinctResult bestResult = null;
        for (FactionInstinctPolicy policy : FactionInstinctPolicies.instinctPolicies()) {
            FactionInstinctPolicy.InstinctResult result = policy.getHighlight(viewer, target, gameComponent);
            if (result != null && (bestResult == null || result.priority() > bestResult.priority())) {
                bestResult = result;
            }
        }
        return Optional.ofNullable(bestResult);
    }
}
