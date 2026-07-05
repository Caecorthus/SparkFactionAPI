package dev.caecorthus.sparkfactionapi.impl.economy;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class FactionPassiveMoneyAdapter {
    private FactionPassiveMoneyAdapter() {
    }

    /**
     * Mirrors Wathe's killer passive-money tick for custom factions that do not enter the native killer path.
     * 为没有进入 wathe 原生杀手路径的自定义阵营镜像杀手被动金钱 tick。
     */
    public static void applyCustomPassiveMoney(ServerWorld serverWorld, GameWorldComponent gameComponent) {
        if (gameComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            return;
        }
        int balanceToAdd = GameConstants.PASSIVE_MONEY_TICKER.apply(serverWorld.getTime());
        if (balanceToAdd <= 0) {
            return;
        }
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!FactionEconomyRules.receivesCustomKillerPassiveMoney(player, gameComponent)) {
                continue;
            }
            PlayerShopComponent shopComponent = PlayerShopComponent.KEY.get(player);
            if (shopComponent.getBalance() < GameConstants.KILLER_PASSIVE_MONEY_CAP) {
                shopComponent.addToBalance(balanceToAdd);
            }
        }
    }
}
