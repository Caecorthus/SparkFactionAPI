package dev.caecorthus.sparkfactionapi.impl.record;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.net.record.MatchRecordNetworking;
import dev.caecorthus.sparkfactionapi.net.record.MatchRecordPayload;
import dev.caecorthus.sparkfactionapi.net.record.MatchRecordSnapshot;
import dev.doctor4t.wathe.api.event.RecordEvents;
import dev.doctor4t.wathe.record.GameRecordManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends each finished Wathe match record to every client that registered {@link MatchRecordPayload}.
 * Wathe fires {@code RecordEvents.ON_RECORD_END} at the end of {@code GameRecordManager.endMatch}, which
 * {@code GameFunctions.finalizeGame} runs before {@code AnnounceEndingPayload} and the INACTIVE game-status sync, so
 * clients hold the record before they see the round end.
 * 把每局结束的 Wathe 对局记录发送给所有注册了 {@link MatchRecordPayload} 的客户端。Wathe 在
 * {@code GameRecordManager.endMatch} 末尾触发 {@code RecordEvents.ON_RECORD_END}，而 {@code GameFunctions.finalizeGame}
 * 在发送 {@code AnnounceEndingPayload} 与同步 INACTIVE 游戏状态之前执行它，因此客户端在看到回合结束前已拿到记录。
 */
public final class MatchRecordBroadcaster {
    /**
     * Same budget as the replay screen snapshot. An oversized custom payload fails to encode and disconnects the
     * player, so the tail is cut here instead.
     * 与回放界面快照相同的预算。超大的自定义数据包编码失败会断开玩家连接，因此在此截掉末尾。
     */
    static final long MAX_ENCODED_BYTES = 900L * 1024L;
    // Match UUID plus the event-count VarInt. 对局 UUID 加事件数 VarInt。
    private static final int HEADER_BYTES = 16 + 5;

    private MatchRecordBroadcaster() {
    }

    public static void register() {
        MatchRecordNetworking.registerCommon();
        RecordEvents.ON_RECORD_END.register(MatchRecordBroadcaster::onRecordEnd);
    }

    private static void onRecordEnd(ServerWorld world, GameRecordManager.MatchRecord match) {
        // Runs inside round finalization: a failure here must never stop the round from ending.
        // 运行于回合收尾流程中：此处失败绝不能阻止回合结束。
        try {
            broadcast(world.getServer(), match);
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.error("Failed to send the match record for match {}", match.getMatchId(), e);
        }
    }

    private static void broadcast(MinecraftServer server, GameRecordManager.MatchRecord match) {
        List<ServerPlayerEntity> receivers = new ArrayList<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, MatchRecordPayload.ID)) {
                receivers.add(player);
            }
        }
        if (receivers.isEmpty()) {
            return;
        }
        MatchRecordPayload payload = new MatchRecordPayload(snapshot(match));
        for (ServerPlayerEntity player : receivers) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static MatchRecordSnapshot snapshot(GameRecordManager.MatchRecord match) {
        MatchRecordRules.Selection selection = MatchRecordRules.select(
                match.getEvents(),
                match.getStartTick(),
                MatchRecordSnapshot.MAX_EVENTS
        );
        List<MatchRecordSnapshot.Event> events = selection.events();
        int fitting = MatchRecordRules.fittingPrefix(
                events,
                MatchRecordBroadcaster::encodedSize,
                MAX_ENCODED_BYTES - HEADER_BYTES
        );
        if (selection.cut() > 0 || fitting < events.size()) {
            SparkFactionApiMod.LOGGER.warn(
                    "Match record {} truncated to its earliest {} events ({} over the {}-event cap, {} over the {}-byte budget)",
                    match.getMatchId(), fitting, selection.cut(), MatchRecordSnapshot.MAX_EVENTS,
                    events.size() - fitting, MAX_ENCODED_BYTES
            );
            events = events.subList(0, fitting);
        }
        return new MatchRecordSnapshot(match.getMatchId(), events);
    }

    private static long encodedSize(MatchRecordSnapshot.Event event) {
        ByteBuf buf = Unpooled.buffer();
        try {
            MatchRecordSnapshot.Event.CODEC.encode(buf, event);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }
}
