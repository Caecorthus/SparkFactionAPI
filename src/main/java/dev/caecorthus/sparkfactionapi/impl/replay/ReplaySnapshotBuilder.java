package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.api.replay.ReplayBadge;
import dev.caecorthus.sparkfactionapi.api.replay.ReplayBadgeContributor;
import dev.caecorthus.sparkfactionapi.api.replay.ReplayPlayerView;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Turns a finished {@link ReplaySession} into the screen {@link ReplaySnapshot} and keeps it within the payload budget.
 * List lengths are clamped to the codec caps here, because the codec rejects (throws on) longer lists.
 * 把已完成的 {@link ReplaySession} 转为界面用 {@link ReplaySnapshot}，并保证不超出数据包预算。列表长度在此按编解码上限截断，
 * 因为编解码遇到超长列表会直接抛异常。
 */
final class ReplaySnapshotBuilder {
    /** Custom payloads are capped at 1 MiB; keep headroom for framing. 自定义数据包上限 1 MiB，为封装留出余量。 */
    static final long MAX_ENCODED_BYTES = 900L * 1024L;
    // Mirrors the private caps of ReplaySnapshot's codec. 与 ReplaySnapshot 编解码的私有上限保持一致。
    static final int MAX_PLAYERS = 256;
    static final int MAX_LINES = 8192;
    static final int MAX_ROLES = 64;
    static final int MAX_BADGES = 32;
    static final int MAX_NAME_LENGTH = 64;

    private ReplaySnapshotBuilder() {
    }

    record Built(ReplaySnapshot snapshot, int encodedBytes) {
    }

    /** Null when even the trimmed snapshot exceeds the budget. 截断后仍超出预算时返回 null。 */
    static @Nullable Built build(ReplaySession session, DynamicRegistryManager registries) {
        ReplaySession.Header header = session.header();
        List<ReplaySnapshot.Player> players = players(session);
        List<ReplaySnapshot.Line> lines = lines(session);
        ReplaySnapshotTrimRules.Fit fit;
        ByteBuf raw = Unpooled.buffer();
        try {
            fit = ReplaySnapshotTrimRules.fit(
                    lines,
                    sizer(snapshot(header, players, List.of()), new RegistryByteBuf(raw, registries)),
                    MAX_ENCODED_BYTES,
                    MAX_LINES
            );
        } finally {
            raw.release();
        }
        if (fit == null) {
            SparkFactionApiMod.LOGGER.warn(
                    "Replay snapshot for match {} exceeds {} bytes even without OTHER/ITEM lines ({} players, {} lines); "
                            + "players will get the chat replay instead",
                    header.matchId(), MAX_ENCODED_BYTES, players.size(), lines.size()
            );
            return null;
        }
        if (!fit.dropped().isEmpty()) {
            SparkFactionApiMod.LOGGER.warn(
                    "Replay snapshot for match {} trimmed to fit {} bytes: dropped {} ({} -> {} lines, {} bytes)",
                    header.matchId(), MAX_ENCODED_BYTES, droppedCounts(lines, fit.dropped()),
                    lines.size(), fit.lines().size(), fit.encodedSize()
            );
        }
        SparkFactionApiMod.LOGGER.info(
                "Replay snapshot for match {} built: {} players, {} lines, {} bytes",
                header.matchId(), players.size(), fit.lines().size(), fit.encodedSize()
        );
        return new Built(snapshot(header, players, fit.lines()), (int) fit.encodedSize());
    }

    /**
     * Runs every badge contributor for one participant in registration order; a contributor that throws is logged and
     * skipped together with the badges it emitted, like {@code ReplayTooltipBuilder#contributorLines}.
     * 按注册顺序为单名参与者执行标签贡献者；抛异常的贡献者会被记录并连同其已输出的标签一起跳过，与
     * {@code ReplayTooltipBuilder#contributorLines} 一致。
     */
    static List<ReplayBadge> contributorBadges(
            ReplayPlayerView view,
            List<Map.Entry<Identifier, ReplayBadgeContributor>> contributors
    ) {
        List<ReplayBadge> badges = new ArrayList<>();
        for (Map.Entry<Identifier, ReplayBadgeContributor> entry : contributors) {
            List<ReplayBadge> own = new ArrayList<>();
            try {
                entry.getValue().contribute(view, badge -> {
                    if (badge != null) {
                        own.add(badge);
                    }
                });
            } catch (RuntimeException | LinkageError e) {
                SparkFactionApiMod.LOGGER.error(
                        "Replay badge contributor {} failed for player {}; skipping it",
                        entry.getKey(),
                        view.uuid(),
                        e
                );
                continue;
            }
            badges.addAll(own);
        }
        return badges;
    }

    private static ReplaySnapshot snapshot(
            ReplaySession.Header header,
            List<ReplaySnapshot.Player> players,
            List<ReplaySnapshot.Line> lines
    ) {
        return new ReplaySnapshot(
                header.matchId(),
                header.gameMode(),
                header.mapEffect(),
                ReplaySummary.secondsBetween(header.startTick(), header.endTick()),
                0,
                header.outcome(),
                players,
                lines
        );
    }

    private static List<ReplaySnapshot.Player> players(ReplaySession session) {
        long startTick = session.header().startTick();
        List<ReplaySnapshot.Player> players = new ArrayList<>();
        for (UUID uuid : session.participants()) {
            if (players.size() >= MAX_PLAYERS) {
                SparkFactionApiMod.LOGGER.warn("Replay snapshot keeps only the first {} players", MAX_PLAYERS);
                break;
            }
            ReplaySession.ScreenFacts facts = session.screenFacts(uuid);
            Text tooltip = session.tooltip(uuid);
            if (facts == null || tooltip == null) {
                continue;
            }
            ReplayPlayerStatusRules.Status status = facts.status();
            players.add(new ReplaySnapshot.Player(
                    uuid,
                    clampName(session.name(uuid)),
                    roleSteps(session, uuid, startTick),
                    facts.faction(),
                    ReplaySnapshot.Presence.valueOf(status.presence().name()),
                    status.deathTick() == null ? -1 : ReplaySummary.secondsBetween(startTick, status.deathTick()),
                    result(status.result()),
                    tooltip,
                    badges(facts.badges())
            ));
        }
        return players;
    }

    /**
     * Never empty; roles held from the start have reachedSecond -1. Over MAX_ROLES, the final role is kept.
     * 不为空；开局即持有的身份 reachedSecond 为 -1。超过 MAX_ROLES 时保留最终身份。
     */
    private static List<ReplaySnapshot.RoleStep> roleSteps(ReplaySession session, UUID uuid, long startTick) {
        List<ReplayRoleChain.Step> steps = session.roleSteps(uuid);
        if (steps.isEmpty()) {
            steps = List.of(new ReplayRoleChain.Step(session.nonEmptyHistory(uuid).getLast(), null));
        }
        if (steps.size() > MAX_ROLES) {
            List<ReplayRoleChain.Step> kept = new ArrayList<>(steps.subList(0, MAX_ROLES - 1));
            kept.add(steps.getLast());
            steps = kept;
        }
        List<ReplaySnapshot.RoleStep> roles = new ArrayList<>(steps.size());
        for (ReplayRoleChain.Step step : steps) {
            Long reached = step.reachedAtTick();
            roles.add(new ReplaySnapshot.RoleStep(
                    step.role(),
                    ReplayNameRenderer.roleName(step.role()),
                    ReplayNameRenderer.roleColor(step.role()),
                    reached == null ? -1 : ReplaySummary.secondsBetween(startTick, reached)
            ));
        }
        return roles;
    }

    private static ReplaySnapshot.Result result(@Nullable ReplayPlayerStatusRules.Result result) {
        if (result == null) {
            return ReplaySnapshot.Result.NONE;
        }
        return result == ReplayPlayerStatusRules.Result.WIN ? ReplaySnapshot.Result.WIN : ReplaySnapshot.Result.LOSE;
    }

    private static List<ReplaySnapshot.Badge> badges(List<ReplayBadge> badges) {
        List<ReplaySnapshot.Badge> converted = new ArrayList<>(Math.min(badges.size(), MAX_BADGES));
        for (ReplayBadge badge : badges) {
            if (converted.size() >= MAX_BADGES) {
                break;
            }
            converted.add(new ReplaySnapshot.Badge(badge.label(), badge.color(), badge.tooltip()));
        }
        return converted;
    }

    private static String clampName(String name) {
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    /**
     * Wathe's formatting order; name hovers become player tags; involved players come from event data plus tagged names.
     * 保持 Wathe 格式化顺序；名字悬停改为玩家标记；涉及玩家取自事件数据与被标记的名字。
     */
    private static List<ReplaySnapshot.Line> lines(ReplaySession session) {
        long startTick = session.header().startTick();
        Function<Style, @Nullable UUID> ownerOf = ReplayTextRewriter.byFrozenTooltip(session.tooltipOwners());
        List<ReplaySnapshot.Line> lines = new ArrayList<>(session.formattedLines().size());
        for (ReplaySession.FormattedLine formatted : session.formattedLines()) {
            List<UUID> mentioned = new ArrayList<>();
            Text text = ReplayTextRewriter.rewrite(formatted.text(), ownerOf, mentioned::add);
            lines.add(new ReplaySnapshot.Line(
                    ReplaySummary.secondsBetween(startTick, formatted.event().worldTick()),
                    ReplayLineCategories.of(formatted.event().type()),
                    ReplayInvolvedPlayers.of(formatted.event().type(), formatted.event().data(), mentioned),
                    text
            ));
        }
        return lines;
    }

    /**
     * Exact {@link ReplaySnapshot#CODEC} size of {@code withoutLines} carrying any subset of lines, without encoding
     * the whole snapshot per candidate. The codec writes the lines list last, as a VarInt count followed by each line
     * through {@link ReplaySnapshot#writeLine} with no state shared between lines, so a candidate costs the line-less
     * size minus its one-byte empty count, plus its own VarInt count and its lines' sizes. Each line is encoded at most
     * once; {@code scratch} is reused for every encode.
     * 精确计算 {@code withoutLines} 搭配任意行子集时的 {@link ReplaySnapshot#CODEC} 字节数，无需为每个候选编码整个快照。
     * 编解码最后写入 lines 列表：先写 VarInt 数量，再经 {@link ReplaySnapshot#writeLine} 逐条独立写入，行之间无共享状态；
     * 因此候选大小 = 无行大小 − 空数量的 1 字节 + 自身 VarInt 数量 + 各行字节数。每行至多编码一次，所有编码复用
     * {@code scratch}。
     */
    static ToLongFunction<List<ReplaySnapshot.Line>> sizer(ReplaySnapshot withoutLines, RegistryByteBuf scratch) {
        long fixed = encodedSize(withoutLines, scratch) - VarInts.getSizeInBytes(0);
        Map<ReplaySnapshot.Line, Integer> lineBytes = new IdentityHashMap<>();
        return candidate -> candidateSize(fixed, candidate, line -> lineBytes.computeIfAbsent(line, key -> {
            scratch.clear();
            ReplaySnapshot.writeLine(scratch, key);
            return scratch.readableBytes();
        }));
    }

    static long candidateSize(long fixed, List<ReplaySnapshot.Line> candidate,
                              ToIntFunction<ReplaySnapshot.Line> lineBytes) {
        long size = fixed + VarInts.getSizeInBytes(candidate.size());
        for (ReplaySnapshot.Line line : candidate) {
            size += lineBytes.applyAsInt(line);
        }
        return size;
    }

    /** Exact encoded size through the payload codec. 经数据包编解码得到的精确字节数。 */
    private static int encodedSize(ReplaySnapshot snapshot, RegistryByteBuf scratch) {
        scratch.clear();
        ReplaySnapshot.CODEC.encode(scratch, snapshot);
        return scratch.readableBytes();
    }

    private static Map<ReplaySnapshot.Category, Integer> droppedCounts(
            List<ReplaySnapshot.Line> lines,
            List<ReplaySnapshot.Category> dropped
    ) {
        Map<ReplaySnapshot.Category, Integer> counts = new EnumMap<>(ReplaySnapshot.Category.class);
        for (ReplaySnapshot.Line line : lines) {
            if (dropped.contains(line.category())) {
                counts.merge(line.category(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
