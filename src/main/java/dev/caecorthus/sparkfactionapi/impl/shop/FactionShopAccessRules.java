package dev.caecorthus.sparkfactionapi.impl.shop;

import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Resolves Wathe killer-shop inventory from original role capabilities, not effective alignment.
 * 通过原始角色能力解析 wathe 杀手商店库存，不使用有效阵营翻转结果。
 */
public final class FactionShopAccessRules {
    private FactionShopAccessRules() {
    }

    public static boolean canUseKillerShop(Role role) {
        return FactionCapabilityLookup.capabilities(role).canUseKillerFeatures();
    }

    public static boolean canUseKillerShop(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null || gameComponent == null) {
            return false;
        }
        return canUseKillerShop(gameComponent.getRole(player));
    }
}
