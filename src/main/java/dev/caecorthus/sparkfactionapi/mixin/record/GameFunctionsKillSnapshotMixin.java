package dev.caecorthus.sparkfactionapi.mixin.record;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparkfactionapi.impl.record.DeathRecordEnricher;
import dev.caecorthus.sparkfactionapi.impl.record.DeathSnapshot;
import dev.caecorthus.sparkfactionapi.impl.record.PlayerScopeStack;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Snapshots victim/killer state at the start of every kill for the {@code death} record. The 3- and 4-argument
 * {@code killPlayer} overloads only delegate to this 5-argument one. {@code @WrapMethod} runs before every HEAD
 * injector (including HEAD cancels from this and other mods) and its {@code finally} also covers those injected
 * returns, Wathe's {@code KillPlayer.BEFORE} cancel, the psycho-shield return and exceptions, so no snapshot outlives
 * its call. Gameplay arguments are passed through unchanged.
 * 为 {@code death} 记录在每次击杀开始时快照受害者/凶手状态。3 参与 4 参的 {@code killPlayer} 重载只是委托到这个 5 参
 * 版本。{@code @WrapMethod} 先于所有 HEAD 注入（包括本模组与其他模组的 HEAD 取消）执行，其 {@code finally} 同样覆盖
 * 这些注入返回、Wathe {@code KillPlayer.BEFORE} 取消、疯魔护盾返回与异常，因此快照不会活过本次调用。玩法参数原样传递。
 */
@Mixin(value = GameFunctions.class, remap = false)
public abstract class GameFunctionsKillSnapshotMixin {
    @WrapMethod(method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V")
    private static void sparkfactionapi$snapshotKillStart(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason,
            boolean force,
            Operation<Void> original
    ) {
        PlayerScopeStack.Frame<DeathSnapshot> frame = DeathRecordEnricher.beginKill(victim, killer);
        try {
            original.call(victim, spawnBody, killer, deathReason, force);
        } finally {
            DeathRecordEnricher.endKill(frame);
        }
    }
}
