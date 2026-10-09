package dev.caecorthus.sparkfactionapi.mixin.record;

import dev.caecorthus.sparkfactionapi.impl.record.IncomeRecorder;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Writes the per-player {@code sparkfactionapi:income} totals at the head of {@code endMatch}, while the match is still
 * active and before Wathe adds {@code match_end}, and forgets older totals once {@code startMatch} has created the new
 * match (after the {@code endMatch} that {@code startMatch} runs for a still-active match). Observes only.
 * 在 {@code endMatch} 头部（对局仍进行中、Wathe 写入 {@code match_end} 之前）写入每名玩家的
 * {@code sparkfactionapi:income} 累计值；并在 {@code startMatch} 创建新对局之后（即其对仍进行中的对局调用的
 * {@code endMatch} 之后）清空旧累计值。仅观察。
 */
@Mixin(value = GameRecordManager.class, remap = false)
public abstract class GameRecordManagerIncomeMixin {
    @Inject(method = "endMatch(Lnet/minecraft/server/world/ServerWorld;)V", at = @At("HEAD"))
    private static void sparkfactionapi$recordIncomeTotals(ServerWorld world, CallbackInfo ci) {
        IncomeRecorder.onMatchEnd(world);
    }

    @Inject(
            method = "startMatch(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;)V",
            at = @At("RETURN")
    )
    private static void sparkfactionapi$forgetOlderIncome(ServerWorld world, GameWorldComponent game, CallbackInfo ci) {
        IncomeRecorder.onMatchStart();
    }
}
