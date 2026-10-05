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

/**
 * {@code /replay} reopens the latest end-of-round replay: the replay screen when the client registered the snapshot
 * payload and a snapshot exists, otherwise the chat replay. {@code /replay chat} always resends the chat replay.
 * Players only, no permission node.
 * {@code /replay} 重新打开最近一局的回放：客户端注册了快照数据包且快照存在时打开回放界面，否则发送聊天回放。
 * {@code /replay chat} 总是重发聊天回放。仅限玩家执行，无权限节点。
 */
public final class ReplayCommand {
    public static final String LITERAL = "replay";
    public static final String CHAT_LITERAL = "chat";

    private ReplayCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal(LITERAL)
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(context -> open(context.getSource()))
                .then(CommandManager.literal(CHAT_LITERAL)
                        .executes(context -> resendChat(context.getSource(), "chat"))));
    }

    private static int open(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ReplayArchive.Entry latest = ReplayArchive.current();
        if (latest.isEmpty()) {
            return none(source, player, "screen");
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
        return resendChat(source, snapshot == null ? "chat-fallback(no snapshot)" : "chat-fallback(no client channel)");
    }

    /** Gold header, then the archived chat replay without the live reopen hint. 金色标题，再发送不含重看提示的聊天回放。 */
    private static int resendChat(ServerCommandSource source, String mode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ReplayArchive.Entry latest = ReplayArchive.current();
        if (latest.chat().isEmpty()) {
            return none(source, player, mode);
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

    private static int none(ServerCommandSource source, ServerPlayerEntity player, String mode) {
        source.sendError(Text.translatable("commands.sparkfactionapi.replay.none"));
        SparkFactionApiMod.LOGGER.info("Replay request from {}: mode={}, no replay stored", player.getGameProfile().getName(), mode);
        return 0;
    }
}
