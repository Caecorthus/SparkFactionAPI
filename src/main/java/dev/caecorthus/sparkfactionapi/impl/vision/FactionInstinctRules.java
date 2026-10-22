package dev.caecorthus.sparkfactionapi.impl.vision;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class FactionInstinctRules {
    private FactionInstinctRules() {
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

    public static int displayColor(PlayerEntity player, GameWorldComponent gameComponent) {
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(player, gameComponent);
        FactionCapabilities capabilities = FactionCapabilityLookup.capabilitiesForFaction(factionId);
        if (capabilities.instinctColor() != -1) {
            return capabilities.instinctColor();
        }
        return FactionRegistryImpl.getFaction(factionId)
                .map(FactionDefinition::color)
                .orElse(-1);
    }

    public static boolean hasCustomInstinctViewer(PlayerEntity viewer, GameWorldComponent gameComponent) {
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(viewer, gameComponent);
        return FactionCapabilityLookup.isCustomFaction(factionId)
                && FactionCapabilityLookup.capabilitiesForFaction(factionId).canUseInstinct();
    }

    public static Optional<FactionInstinctPolicy.InstinctResult> policyResult(
            PlayerEntity viewer,
            Entity target,
            GameWorldComponent gameComponent
    ) {
        return Optional.ofNullable(policyResultOrNull(viewer, target, gameComponent));
    }

    /**
     * Allocation-free form of {@link #policyResult} for Wathe's per-frame instinct highlight event; null means no
     * policy answered.
     * 供 wathe 每帧直觉高亮事件使用的无分配版本 {@link #policyResult}；返回 null 表示没有策略给出结果。
     */
    public static FactionInstinctPolicy.@Nullable InstinctResult policyResultOrNull(
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
        return bestResult;
    }
}
