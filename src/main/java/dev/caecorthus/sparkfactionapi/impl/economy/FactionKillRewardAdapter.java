package dev.caecorthus.sparkfactionapi.impl.economy;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Adapts Wathe kill hooks into SparkFactionAPI custom kill-reward compensation.
 * 将 Wathe 击杀钩子适配到 SparkFactionAPI 的自定义击杀奖励补偿。
 */
public final class FactionKillRewardAdapter {
    private FactionKillRewardAdapter() {
    }

    public static void applyCustomKillReward(ServerPlayerEntity victim, @Nullable ServerPlayerEntity directKiller) {
        ServerPlayerEntity moneyRecipient = rewardRecipient(victim, directKiller);
        if (moneyRecipient == null) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(moneyRecipient.getWorld());
        if (FactionEconomyRules.receivesCustomKillReward(moneyRecipient, gameComponent)) {
            PlayerShopComponent.KEY.get(moneyRecipient).addToBalance(GameConstants.MONEY_PER_KILL);
        }
    }

    private static @Nullable ServerPlayerEntity rewardRecipient(
            ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity directKiller
    ) {
        if (directKiller != null) {
            return directKiller;
        }

        PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(victim);
        MinecraftServer server = victim.getServer();
        if (poisonComponent.poisoner == null || server == null) {
            return null;
        }
        return server.getPlayerManager().getPlayer(poisonComponent.poisoner);
    }
}
