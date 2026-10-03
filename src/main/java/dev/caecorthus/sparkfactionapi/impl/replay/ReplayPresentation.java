package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.command.replay.ReplayCommand;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
 * Presentation side of the replay module: time-aware names, hover tooltips, roster, archive, and /replay.
 * The {@code ReplayGeneratorMixin} adapter only forwards Wathe seams here; every hook degrades to Wathe's
 * original behaviour when no session is active or presentation code fails, so the replay itself never breaks.
 * 回放模块的展示侧：按时间解析的名字、悬停提示、名单、存档与 /replay 命令。{@code ReplayGeneratorMixin}
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
            List<Text> body = session.generatedLines();
            if (body != null) {
                ReplayArchive.store(ReplayArchive.frame(body, reopenHint()));
            } else {
                ReplayArchive.clear();
            }
        }
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

    /** Sent after Wathe's footer to each recipient. 在 Wathe 结尾之后发送给每名接收者。 */
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
