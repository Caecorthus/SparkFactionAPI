package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.command.replay.ReplayCommand;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshotPayload;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Presentation side of the replay module: time-aware names, hover tooltips, roster, end-of-match summary, screen
 * snapshot, archive, and /replay.
 * The {@code ReplayGeneratorMixin} adapter only forwards Wathe seams here; every hook degrades to Wathe's
 * original behaviour when no session is active or presentation code fails, so the replay itself never breaks.
 * 回放模块的展示侧：按时间解析的名字、悬停提示、名单、局末摘要、界面快照、存档与 /replay 命令。{@code ReplayGeneratorMixin}
 * 适配器只把 Wathe 接缝转发到这里；没有活动会话或展示代码出错时，每个钩子都退回 Wathe 原行为，回放本身不会中断。
 */
public final class ReplayPresentation {
    public static final String REOPEN_COMMAND = "/replay";

    private ReplayPresentation() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ReplayCommand.register(dispatcher));
        // The archive is static; drop it so an integrated server never shows another world's replay.
        // 存档为静态状态；服务器停止时清空，避免单人模式切换世界后看到其他世界的回放。
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ReplayArchive.clear());
    }

    /** Wraps {@code ReplayGenerator.generateAndSend}. 包裹 {@code ReplayGenerator.generateAndSend}。 */
    public static void generate(ServerWorld world, GameRecordManager.MatchRecord match, Runnable original) {
        ReplaySession session;
        try {
            session = ReplaySession.build(world, match);
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.error("Failed to prepare replay presentation; sending the plain Wathe replay", e);
            ReplayArchive.clear();
            original.run();
            return;
        }
        ReplayRenderContext.install(session);
        try {
            original.run();
        } finally {
            ReplayRenderContext.clear();
            archive(session, world);
        }
    }

    /**
     * Stores the chat lines with the optional screen snapshot: if the snapshot fails or is too large, the archive
     * keeps only chat and /replay falls back to it.
     * 存入聊天行及可选的界面快照：快照构建失败或过大时存档只保留聊天，/replay 退回聊天回放。
     */
    private static void archive(ReplaySession session, ServerWorld world) {
        List<Text> body = session.generatedLines();
        if (body == null) {
            ReplayArchive.clear();
            return;
        }
        long endedAtMillis = System.currentTimeMillis();
        ReplaySnapshotBuilder.Built built = null;
        try {
            built = ReplaySnapshotBuilder.build(session, world.getServer().getRegistryManager());
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.error("Failed to build the replay screen snapshot; /replay will resend chat", e);
        }
        ReplayArchive.store(
                ReplayArchive.frame(body),
                built == null ? null : built.snapshot(),
                built == null ? 0 : built.encodedBytes(),
                endedAtMillis
        );
    }

    /**
     * Captures one formatted line for the snapshot; never breaks Wathe's generation.
     * 为快照记录一行已格式化内容；不会中断 Wathe 的生成。
     */
    public static void recordFormattedLine(GameRecordManager.MatchRecord match, GameRecordEvent event, Text text) {
        ReplaySession session = ReplayRenderContext.session();
        if (session == null || !session.isFor(match)) {
            return;
        }
        try {
            session.recordFormattedLine(event, text);
        } catch (RuntimeException e) {
            if (session.markRenderFailureLogged()) {
                SparkFactionApiMod.LOGGER.error("Failed to capture a replay line for the screen snapshot", e);
            }
        }
    }

    /**
     * Sends the short summary in place of Wathe's full chat replay when a session is active and the player's client
     * registered the snapshot payload. Returns true when sent, so the caller cancels Wathe's chat for this player.
     * 会话有效且玩家客户端注册了快照数据包时，用简短摘要替代 Wathe 完整聊天回放。已发送时返回 true，由调用方取消该玩家的聊天回放。
     */
    public static boolean sendSummaryInstead(ServerPlayerEntity player) {
        ReplaySession session = ReplayRenderContext.session();
        if (session == null) {
            return false;
        }
        List<Text> summary;
        try {
            if (!ServerPlayNetworking.canSend(player, ReplaySnapshotPayload.ID)) {
                return false;
            }
            summary = session.summaryLines();
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.error("Failed to build the replay summary; sending the full chat replay", e);
            return false;
        }
        for (Text line : summary) {
            player.sendMessage(line, false);
        }
        return true;
    }

    /** Session name for {@code formatPlayerName}; null runs Wathe's original. 会话渲染的名字；null 表示执行 Wathe 原逻辑。 */
    public static @Nullable Text sessionName(@Nullable UUID uuid) {
        ReplaySession session = ReplayRenderContext.session();
        if (session == null || uuid == null) {
            return null;
        }
        try {
            return ReplayNameRenderer.nameWithRole(uuid);
        } catch (RuntimeException e) {
            if (session.markRenderFailureLogged()) {
                SparkFactionApiMod.LOGGER.error("Replay name rendering failed; falling back to Wathe names", e);
            }
            return null;
        }
    }

    /** Once-built player map for this match; null lets Wathe rebuild. 本局一次性构建的玩家表；null 表示交由 Wathe 重建。 */
    public static @Nullable Map<UUID, ReplayGenerator.PlayerInfo> sessionPlayerInfo(GameRecordManager.MatchRecord match) {
        ReplaySession session = ReplayRenderContext.session();
        return session != null && session.isFor(match) ? session.playerInfoCopy() : null;
    }

    /** Prepends the roster and keeps the body for the archive. 在 Wathe 生成的行前插入名单，并保存正文供存档。 */
    public static List<Text> prependRoster(GameRecordManager.MatchRecord match, List<Text> lines) {
        ReplaySession session = ReplayRenderContext.session();
        if (session == null || !session.isFor(match)) {
            return lines;
        }
        List<Text> body;
        try {
            List<Text> roster = session.rosterLines();
            body = new ArrayList<>(roster.size() + lines.size());
            body.addAll(roster);
            body.addAll(lines);
        } catch (RuntimeException e) {
            SparkFactionApiMod.LOGGER.error("Failed to build the replay roster; sending events only", e);
            body = new ArrayList<>(lines);
        }
        session.recordGeneratedLines(body);
        return body;
    }

    /**
     * Sent after Wathe's footer to each full-chat recipient; summary recipients never reach it.
     * 在 Wathe 结尾之后发送给每名完整聊天接收者；摘要接收者不会走到这里。
     */
    public static void sendReopenHint(ServerPlayerEntity player) {
        if (ReplayRenderContext.session() != null) {
            player.sendMessage(reopenHint(), false);
        }
    }

    static Text reopenHint() {
        return Text.translatable("replay.sparkfactionapi.hint.reopen")
                .formatted(Formatting.GRAY)
                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, REOPEN_COMMAND)));
    }
}
