package dev.caecorthus.sparkfactionapi.command.replay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.impl.replay.ReplayArchive;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshotPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /replay} reopens the latest end-of-round replay: the replay screen when the client registered the snapshot
 * payload and a snapshot exists, otherwise the chat replay. {@code /replay chat} always resends the chat replay.
 * Players only, no permission node.
 * {@code /replay} 重新打开最近一局的回放：客户端注册了快照数据包且快照存在时打开回放界面，否则发送聊天回放。
 * {@code /replay chat} 总是重发聊天回放。仅限玩家执行，无权限节点。
 *
 * <p>Each answered request re-encodes the whole snapshot (up to the replay byte budget) on the connection's event
 * loop, or sends one chat packet per archived line, so the server throttles requests per player. The client's own
 * key cooldown is only a courtesy; a modified client can send the command directly.
 * 每次应答都会在连接的事件循环上重新编码整份快照（最大为回放字节预算），或为每行存档各发一个聊天包，
 * 因此服务端按玩家限流。客户端自己的按键冷却只是礼貌性的；改过的客户端可以直接发送命令。
 */
public final class ReplayCommand {
    public static final String LITERAL = "replay";
    public static final String CHAT_LITERAL = "chat";
    /**
     * Minimum server ticks between two answered requests of one player. Longer than the client's key cooldown, short
     * enough that reopening a just-closed replay only needs a second press.
     * 同一玩家两次被应答的请求之间的最少服务端 tick 数。长于客户端按键冷却，又足够短，重开刚关闭的回放只需再按一次。
     */
    public static final int REQUEST_COOLDOWN_TICKS = 40;

    /** Server tick of each player's last answered request; server thread only. / 每名玩家上次被应答请求的服务端 tick；仅服务端线程访问。 */
    private static final Map<UUID, Integer> LAST_ANSWERED_TICK = new HashMap<>();

    private ReplayCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal(LITERAL)
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(context -> open(context.getSource()))
                .then(CommandManager.literal(CHAT_LITERAL)
                        .executes(context -> resendChat(context.getSource(), "chat", false))));
    }

    private static int open(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ReplayArchive.Entry latest = ReplayArchive.current();
        if (latest.isEmpty()) {
            return none(source, player, "screen");
        }
        if (throttled(source, player)) {
            return 0;
        }
        ReplaySnapshot snapshot = latest.snapshot();
        if (snapshot != null && ServerPlayNetworking.canSend(player, ReplaySnapshotPayload.ID)) {
            int endedSecondsAgo = latest.endedSecondsAgo(System.currentTimeMillis());
            ServerPlayNetworking.send(player, new ReplaySnapshotPayload(snapshot.withEndedSecondsAgo(endedSecondsAgo)));
            SparkFactionApiMod.LOGGER.info(
                    "Replay request from {}: mode=screen, {} players, {} lines, ~{} bytes, ended {}s ago",
                    player.getGameProfile().getName(),
                    snapshot.players().size(),
                    snapshot.lines().size(),
                    latest.snapshotBytes(),
                    endedSecondsAgo
            );
            return 1;
        }
        return resendChat(
                source,
                snapshot == null ? "chat-fallback(no snapshot)" : "chat-fallback(no client channel)",
                true
        );
    }

    /** Gold header, then the archived chat replay without the live reopen hint. 金色标题，再发送不含重看提示的聊天回放。 */
    private static int resendChat(ServerCommandSource source, String mode, boolean alreadyAdmitted)
            throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ReplayArchive.Entry latest = ReplayArchive.current();
        if (latest.chat().isEmpty()) {
            return none(source, player, mode);
        }
        // The screen path already passed the throttle before falling back to chat; only a direct /replay chat is
        // checked here, so one request is never counted twice.
        // 界面路径在回退到聊天之前已通过限流；这里只检查直接执行的 /replay chat，同一请求不会被计两次。
        if (!alreadyAdmitted && throttled(source, player)) {
            return 0;
        }
        player.sendMessage(Text.translatable("replay.sparkfactionapi.resend.header").formatted(Formatting.GOLD), false);
        for (Text line : latest.chat()) {
            player.sendMessage(line, false);
        }
        SparkFactionApiMod.LOGGER.info(
                "Replay request from {}: mode={}, {} chat lines",
                player.getGameProfile().getName(),
                mode,
                latest.chat().size()
        );
        return latest.chat().size();
    }

    /**
     * True, with an error line, when this player was answered less than {@link #REQUEST_COOLDOWN_TICKS} ago; otherwise
     * stamps the request.
     * 该玩家在 {@link #REQUEST_COOLDOWN_TICKS} 内已被应答过时返回 true 并提示；否则记下本次请求。
     */
    private static boolean throttled(ServerCommandSource source, ServerPlayerEntity player) {
        int now = source.getServer().getTicks();
        // Entries that can no longer throttle are dropped here, so the map never outgrows the players who asked
        // within one cooldown. A stamp from the future is a previous integrated-server run: also dropped.
        // 已无法再限流的条目在此丢弃，因此映射不会超过一个冷却期内请求过的玩家数。
        // 来自"未来"的记录属于上一次内置服务器运行：同样丢弃。
        LAST_ANSWERED_TICK.values().removeIf(tick -> !ReplayCommand.withinCooldown(tick, now));
        if (LAST_ANSWERED_TICK.containsKey(player.getUuid())) {
            source.sendError(Text.translatable("commands.sparkfactionapi.replay.cooldown"));
            return true;
        }
        LAST_ANSWERED_TICK.put(player.getUuid(), now);
        return false;
    }

    /** Pure rule: whether a request stamped at {@code lastTick} still throttles at {@code now}. / 纯规则：{@code lastTick} 的记录在 {@code now} 是否仍然限流。 */
    static boolean withinCooldown(int lastTick, int now) {
        return lastTick <= now && now - lastTick < REQUEST_COOLDOWN_TICKS;
    }

    private static int none(ServerCommandSource source, ServerPlayerEntity player, String mode) {
        source.sendError(Text.translatable("commands.sparkfactionapi.replay.none"));
        SparkFactionApiMod.LOGGER.info("Replay request from {}: mode={}, no replay stored", player.getGameProfile().getName(), mode);
        return 0;
    }
}
