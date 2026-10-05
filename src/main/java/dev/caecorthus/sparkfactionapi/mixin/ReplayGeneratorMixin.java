package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparkfactionapi.impl.replay.ReplayPresentation;
import dev.caecorthus.sparkfactionapi.impl.replay.ReplayRenderContext;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayEventFormatter;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter for Wathe's end-of-round replay. Wathe runs {@code generateAndSend} synchronously from
 * {@code GameRecordManager.endMatch}, before players and roles are reset, so the whole generation is wrapped in a
 * thread-local session that every add-on formatter reaches through the shared static {@code formatPlayerName}.
 * Wathe 局末回放的适配器。Wathe 在 {@code GameRecordManager.endMatch} 中同步执行 {@code generateAndSend}，
 * 早于玩家与身份重置；因此整个生成过程被包在线程内会话中，所有附属模组的格式化器经由共享的静态
 * {@code formatPlayerName} 进入该会话。
 */
@Mixin(value = ReplayGenerator.class, remap = false)
public abstract class ReplayGeneratorMixin {
    @WrapMethod(
            method = "generateAndSend(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/record/GameRecordManager$MatchRecord;)V"
    )
    private static void sparkfactionapi$presentReplay(
            ServerWorld world,
            GameRecordManager.MatchRecord match,
            Operation<Void> original
    ) {
        ReplayPresentation.generate(world, match, () -> original.call(world, match));
    }

    @WrapOperation(
            method = "generateReplayLines(Ldev/doctor4t/wathe/record/GameRecordManager$MatchRecord;Lnet/minecraft/server/world/ServerWorld;Ljava/util/Map;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/record/replay/ReplayEventFormatter;format(Ldev/doctor4t/wathe/record/GameRecordEvent;Ldev/doctor4t/wathe/record/GameRecordManager$MatchRecord;Lnet/minecraft/server/world/ServerWorld;)Lnet/minecraft/text/Text;"
            )
    )
    private static Text sparkfactionapi$trackFormattedEvent(
            ReplayEventFormatter formatter,
            GameRecordEvent event,
            GameRecordManager.MatchRecord match,
            ServerWorld world,
            Operation<Text> original
    ) {
        // Names rendered inside this call use the role held at this event's sequence number; each non-null result is
        // also captured, in Wathe's order, for the replay screen snapshot.
        // 本次调用内渲染的名字使用该事件序号时刻的身份；每个非 null 结果也按 Wathe 顺序记录，供回放界面快照使用。
        GameRecordEvent previous = ReplayRenderContext.swapCurrentEvent(event);
        try {
            Text formatted = original.call(formatter, event, match, world);
            if (formatted != null) {
                ReplayPresentation.recordFormattedLine(match, event, formatted);
            }
            return formatted;
        } finally {
            ReplayRenderContext.swapCurrentEvent(previous);
        }
    }

    @Inject(
            method = "formatPlayerName(Ljava/util/UUID;Ljava/util/Map;)Lnet/minecraft/text/Text;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sparkfactionapi$renderSessionName(
            UUID uuid,
            Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache,
            CallbackInfoReturnable<Text> cir
    ) {
        Text name = ReplayPresentation.sessionName(uuid);
        if (name != null) {
            cir.setReturnValue(name);
        }
    }

    @Inject(
            method = "buildPlayerInfoCache(Ldev/doctor4t/wathe/record/GameRecordManager$MatchRecord;)Ljava/util/Map;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sparkfactionapi$reuseSessionPlayerInfo(
            GameRecordManager.MatchRecord match,
            CallbackInfoReturnable<Map<UUID, ReplayGenerator.PlayerInfo>> cir
    ) {
        // Covers both generateAndSend and the public getPlayerInfoCache, which add-on formatters call per event
        // and which otherwise rescans every record event each time.
        // 同时覆盖 generateAndSend 与公开的 getPlayerInfoCache；附属格式化器每个事件都会调用后者，原实现每次都重扫全部事件。
        Map<UUID, ReplayGenerator.PlayerInfo> cached = ReplayPresentation.sessionPlayerInfo(match);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @ModifyReturnValue(
            method = "generateReplayLines(Ldev/doctor4t/wathe/record/GameRecordManager$MatchRecord;Lnet/minecraft/server/world/ServerWorld;Ljava/util/Map;)Ljava/util/List;",
            at = @At("RETURN")
    )
    private static List<Text> sparkfactionapi$prependRoster(
            List<Text> lines,
            @Local(argsOnly = true) GameRecordManager.MatchRecord match
    ) {
        return ReplayPresentation.prependRoster(match, lines);
    }

    /**
     * Wathe calls this once per online player. Players whose client can receive the replay snapshot get a short
     * summary with buttons instead; cancelling returns before the TAIL hint below, so they never get that hint.
     * Wathe 对每名在线玩家调用一次。客户端能接收回放快照的玩家改为收到带按钮的简短摘要；取消后直接返回，不会执行下方
     * TAIL 处的重看提示。
     */
    @Inject(
            method = "sendReplayToPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/List;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sparkfactionapi$sendSummaryInstead(
            ServerPlayerEntity player,
            List<Text> replayLines,
            CallbackInfo ci
    ) {
        if (ReplayPresentation.sendSummaryInstead(player)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "sendReplayToPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/List;)V",
            at = @At("TAIL")
    )
    private static void sparkfactionapi$appendReopenHint(
            ServerPlayerEntity player,
            List<Text> replayLines,
            CallbackInfo ci
    ) {
        ReplayPresentation.sendReopenHint(player);
    }
}
