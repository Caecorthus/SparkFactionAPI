package dev.caecorthus.sparkfactionapi.impl.blackout;

import dev.caecorthus.sparkfactionapi.mixin.PlayerShopComponentMixin;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Adapts Wathe blackout shop purchases to custom-faction cooldown sharing.
 * 将 wathe 熄灯商店购买适配到自定义阵营冷却共享语义。
 */
public final class FactionBlackoutCooldownAdapter {
    private static final String BLACKOUT_SHOP_ENTRY_ID = "blackout";
    private static final String BLACKOUT_COOLDOWN_ID = "blackout";

    private FactionBlackoutCooldownAdapter() {
    }

    public static void afterShopPurchase(ServerPlayerEntity purchaser, ShopEntry entry, int index, int pricePaid) {
        if (!BLACKOUT_SHOP_ENTRY_ID.equals(entry.id())) {
            return;
        }

        // Wathe and SparkTraits have already shared native killer cooldowns; add only custom bridges here.
        // wathe 与 SparkTraits 已经分发原生杀手冷却；这里仅追加自定义阵营桥接。
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(purchaser.getWorld());
        int cooldownTicks = GameConstants.getInTicks(5, 0);
        for (ServerPlayerEntity player : purchaser.getServerWorld().getPlayers()) {
            Boolean cooldownOverride = FactionBlackoutRules.blackoutCooldownOverride(purchaser, player, gameComponent);
            if (cooldownOverride != null) {
                setBlackoutCooldown(player, cooldownOverride, cooldownTicks);
                continue;
            }
            if (FactionBlackoutRules.sharesCustomBlackoutCooldown(player, gameComponent)) {
                setBlackoutCooldown(player, true, cooldownTicks);
            }
        }
    }

    private static void setBlackoutCooldown(ServerPlayerEntity player, boolean enabled, int cooldownTicks) {
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (enabled) {
            ((PlayerShopComponentMixin) (Object) shop).sparkfactionapi$getCooldowns()
                    .put(BLACKOUT_COOLDOWN_ID, cooldownTicks);
        } else {
            ((PlayerShopComponentMixin) (Object) shop).sparkfactionapi$getCooldowns()
                    .remove(BLACKOUT_COOLDOWN_ID);
        }
        shop.sync();
    }
}
