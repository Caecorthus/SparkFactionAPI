package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.economy.FactionPassiveMoneyAdapter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapts Wathe's server-game-loop tick to custom faction passive-money compensation.
 * 将 wathe 服务端游戏循环 tick 适配到自定义阵营被动金钱补偿。
 */
@Mixin(value = MurderGameMode.class, remap = false)
public abstract class MurderGameModePassiveMoneyMixin {
    @Inject(
            method = "tickServerGameLoop",
            at = @At("HEAD")
    )
    private void sparkfactionapi$applyCustomPassiveMoney(
            ServerWorld serverWorld,
            GameWorldComponent gameComponent,
            CallbackInfo ci
    ) {
        FactionPassiveMoneyAdapter.applyCustomPassiveMoney(serverWorld, gameComponent);
    }
}
