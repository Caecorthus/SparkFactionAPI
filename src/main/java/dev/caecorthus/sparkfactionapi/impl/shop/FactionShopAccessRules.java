package dev.caecorthus.sparkfactionapi.impl.shop;

import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Resolves Wathe killer-shop access through SparkFactionAPI faction capabilities.
 * 通过 SparkFactionAPI 阵营能力解析 wathe 杀手商店访问权限。
 */
public final class FactionShopAccessRules {
    private FactionShopAccessRules() {
    }

    public static boolean canUseKillerShop(Role role) {
        return FactionCapabilityLookup.capabilities(role).canUseKillerFeatures();
    }

    public static boolean canUseKillerShop(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityLookup.capabilities(player, gameComponent).canUseKillerFeatures();
    }
}
