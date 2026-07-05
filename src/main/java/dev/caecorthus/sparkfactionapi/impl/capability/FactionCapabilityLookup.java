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

    public static boolean hasCustomBaseFaction(Role role) {
        return FactionRegistryImpl.isCustomFaction(baseFaction(role));
    }

    public static boolean hasCustomEffectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null || gameComponent == null) {
            return false;
        }
        return FactionRegistryImpl.isCustomFaction(effectiveFaction(player, gameComponent));
    }
}
