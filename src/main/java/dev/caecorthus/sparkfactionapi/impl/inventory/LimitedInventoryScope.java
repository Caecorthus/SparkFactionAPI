package dev.caecorthus.sparkfactionapi.impl.inventory;

import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Whether a player currently uses Wathe's limited inventory; mirrors the condition under which Wathe swaps in
 * {@code LimitedInventoryScreen} and guards in-screen drops.
 * 玩家当前是否使用 Wathe 受限物品栏；与 Wathe 替换 {@code LimitedInventoryScreen} 及拦截界面内丢弃的条件一致。
 */
public final class LimitedInventoryScope {
    private LimitedInventoryScope() {
    }

    public static boolean appliesTo(PlayerEntity player) {
        if (player == null || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }
        TrainWorldComponent train = TrainWorldComponent.KEY.getNullable(player.getWorld());
        return train != null && train.hasHud();
    }
}
