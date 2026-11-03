package dev.caecorthus.sparkfactionapi.mixin.mapreset;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparkfactionapi.impl.mapreset.LobbyMapReset;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.MapEffect;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adapts Wathe's start and train-copy functions to the lobby map reset.
 * 将 Wathe 的开局与车厢复制函数适配到大厅地图重置。
 */
@Mixin(value = GameFunctions.class, remap = false)
public abstract class GameFunctionsMapResetMixin {
    @Inject(
            method = "startGame(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/api/GameMode;Ldev/doctor4t/wathe/api/MapEffect;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sparkfactionapi$resetMapBeforeStart(
            ServerWorld world,
            GameMode gameMode,
            MapEffect mapEffect,
            int time,
            CallbackInfo ci
    ) {
        // Before Wathe stores the mode, effect and time, so a deferred or refused start changes nothing yet.
        // 在 Wathe 保存模式、效果与时长之前处理，被推迟或拒绝的开局暂时不会改变任何设置。
        if (LobbyMapReset.interceptStart(world, gameMode, mapEffect, time)) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "tryResetTrain(Lnet/minecraft/server/world/ServerWorld;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/MapVariablesWorldComponent;getResetTemplateArea()Lnet/minecraft/util/math/Box;"
            )
    )
    private static Box sparkfactionapi$narrowPreparedCopy(
            MapVariablesWorldComponent areas,
            Operation<Box> original,
            @Local(argsOnly = true) ServerWorld world
    ) {
        // Every template read in the copy is narrowed alike, so corners, offset and size stay consistent.
        // 复制中每次读取模板都同样缩小，角点、偏移与尺寸保持一致。
        return LobbyMapReset.templateForWatheCopy(world, areas, original.call(areas));
    }

    @Inject(method = "tryResetTrain(Lnet/minecraft/server/world/ServerWorld;)Z", at = @At("RETURN"))
    private static void sparkfactionapi$consumePreparedCopy(ServerWorld world, CallbackInfoReturnable<Boolean> cir) {
        LobbyMapReset.onWatheCopyFinished(world, cir.getReturnValueZ());
    }
}
