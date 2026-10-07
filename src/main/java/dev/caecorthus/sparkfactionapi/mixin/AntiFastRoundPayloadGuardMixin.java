package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparkfactionapi.impl.antifastround.AntiFastRound;
import dev.caecorthus.sparkfactionapi.impl.antifastround.AntiFastRoundRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.server.ServerPlayNetworkAddon;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * External seam (Fabric networking internals; the same seam SparkWitch's Control Expert stun guard and SparkStrength's
 * Taotie daze guard wrap, and MixinExtras wrappers chain): wraps the single main-thread hand-off in {@code receive} so
 * a skill payload sent during the safe time is dropped on the server thread, atomically with the handler it would have
 * run. Only ids in {@link AntiFastRoundRules#BLOCKED_PAYLOADS} are wrapped; every other payload is scheduled unchanged.
 * Server authority: a modified client cannot bypass the safe time.
 * 外部接缝（Fabric 网络内部实现；与 SparkWitch 控场专家眩晕、SparkStrength 饕餮眩晕包装的是同一接缝，MixinExtras
 * 包装会串联）：包装 {@code receive} 中唯一一次移交主线程的调用，使安全时间内发送的技能数据包在服务端主线程上、
 * 与其原本要执行的处理器原子地被丢弃。仅包装 {@link AntiFastRoundRules#BLOCKED_PAYLOADS} 中的 id；其他数据包原样调度。
 * 服务端权威：修改过的客户端无法绕过安全时间。
 */
@Mixin(value = ServerPlayNetworkAddon.class, remap = false)
public abstract class AntiFastRoundPayloadGuardMixin {
    @Shadow
    @Final
    private ServerPlayNetworking.Context context;

    @WrapOperation(
            method = "receive(Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$PlayPayloadHandler;"
                    + "Lnet/minecraft/network/packet/CustomPayload;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;execute(Ljava/lang/Runnable;)V")
    )
    private void sparkfactionapi$dropSafeTimePayload(
            MinecraftServer server,
            Runnable handler,
            Operation<Void> original,
            @Local(argsOnly = true) CustomPayload payload
    ) {
        Identifier payloadId = payload == null || payload.getId() == null ? null : payload.getId().id();
        if (!AntiFastRoundRules.isBlockedPayload(payloadId)) {
            original.call(server, handler);
            return;
        }
        original.call(server, (Runnable) () -> {
            // Re-checked on the server thread, where the window and the player state are authoritative.
            // 在服务端主线程上重新判断，此时窗口与玩家状态才是权威的。
            ServerPlayerEntity player = context.player();
            if (AntiFastRound.isRestricted(player)) {
                AntiFastRound.notifyBlocked(player);
                return;
            }
            handler.run();
        });
    }
}
