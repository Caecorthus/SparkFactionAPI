package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.impl.blackout.FactionBlackoutCooldownAdapter;
import dev.caecorthus.sparkfactionapi.impl.gun.FactionGunPunishmentAdapter;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;

/**
 * Event-backed compatibility hooks that avoid owning Wathe predicate redirects.
 * 基于事件的兼容钩子，避免独占 wathe 的阵营判断调用点。
 */
public final class FactionCompatibilityEvents {
    private static boolean registered;

    private FactionCompatibilityEvents() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        ShopPurchase.AFTER.register(FactionBlackoutCooldownAdapter::afterShopPurchase);
        ShouldPunishGunShooter.EVENT.register(FactionGunPunishmentAdapter::shouldPunishGunShooter);
    }
}
