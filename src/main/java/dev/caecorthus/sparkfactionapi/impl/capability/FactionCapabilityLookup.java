package dev.caecorthus.sparkfactionapi.impl.capability;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Centralizes internal base/effective faction capability lookup without owning gameplay policy.
 * 集中内部基础/有效阵营能力查询，但不持有玩法策略。
 */
public final class FactionCapabilityLookup {
    private FactionCapabilityLookup() {
    }

    public static Identifier baseFaction(Role role) {
        return FactionRegistryImpl.resolveBaseFaction(role);
    }

    public static Identifier effectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent);
    }

    public static FactionCapabilities capabilities(Role role) {
        return FactionRegistryImpl.capabilities(baseFaction(role));
    }

    public static FactionCapabilities capabilities(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionRegistryImpl.capabilities(effectiveFaction(player, gameComponent));
    }

    public static boolean hasCustomEffectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null || gameComponent == null) {
            return false;
        }
        return FactionRegistryImpl.isCustomFaction(effectiveFaction(player, gameComponent));
    }

    /**
     * Capabilities of an already-resolved faction, so a rule can resolve a player's effective faction once and
     * reuse it. For null inputs {@link #effectiveFaction} yields {@code wathe:none}, which is never custom, so
     * pairing this with {@link #isCustomFaction} matches {@link #hasCustomEffectiveFaction}'s null guard.
     * 已解析阵营的能力，供规则对同一玩家只解析一次有效阵营后复用。输入为 null 时 effectiveFaction 返回
     * wathe:none，它永远不是自定义阵营，因此与 isCustomFaction 搭配等价于 hasCustomEffectiveFaction 的 null 保护。
     */
    public static FactionCapabilities capabilitiesForFaction(Identifier factionId) {
        return FactionRegistryImpl.capabilities(factionId);
    }

    public static boolean isCustomFaction(Identifier factionId) {
        return FactionRegistryImpl.isCustomFaction(factionId);
    }
}
