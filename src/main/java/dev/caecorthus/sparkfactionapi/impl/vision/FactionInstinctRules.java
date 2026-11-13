package dev.caecorthus.sparkfactionapi.impl.vision;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionCatalog;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FactionInstinctRules {
    // Owns instinct policy registration and priority evaluation inputs. Copy-on-write, so an evaluation iterates a
    // snapshot and a policy registered mid-pass neither throws nor joins that pass.
    // 持有直觉策略的注册与优先级评估输入。写时复制，评估遍历快照，评估中途注册的策略既不抛异常也不加入本轮。
    private static final List<FactionInstinctPolicy> INSTINCT_POLICIES = new CopyOnWriteArrayList<>();

    private FactionInstinctRules() {
    }

    public static void register(FactionInstinctPolicy policy) {
        INSTINCT_POLICIES.add(policy);
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
        return FactionCatalog.getFaction(factionId)
                .map(FactionDefinition::color)
                .orElse(-1);
    }

    public static boolean hasCustomInstinctViewer(PlayerEntity viewer, GameWorldComponent gameComponent) {
        Identifier factionId = FactionCapabilityLookup.effectiveFaction(viewer, gameComponent);
        return FactionCapabilityLookup.isCustomFaction(factionId)
                && FactionCapabilityLookup.capabilitiesForFaction(factionId).canUseInstinct();
    }

    /**
     * Allocation-free policy lookup for Wathe's per-frame instinct highlight event; null means no policy answered.
     * 供 wathe 每帧直觉高亮事件使用的无分配策略查询；返回 null 表示没有策略给出结果。
     */
    public static FactionInstinctPolicy.@Nullable InstinctResult policyResultOrNull(
            PlayerEntity viewer,
            Entity target,
            GameWorldComponent gameComponent
    ) {
        FactionInstinctPolicy.InstinctResult bestResult = null;
        for (FactionInstinctPolicy policy : INSTINCT_POLICIES) {
            FactionInstinctPolicy.InstinctResult result = policy.getHighlight(viewer, target, gameComponent);
            if (result != null && (bestResult == null || result.priority() > bestResult.priority())) {
                bestResult = result;
            }
        }
        return bestResult;
    }
}
