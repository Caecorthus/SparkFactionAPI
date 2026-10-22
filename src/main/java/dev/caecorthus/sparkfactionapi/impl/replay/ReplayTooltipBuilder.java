package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.api.replay.ReplayPlayerView;
import dev.caecorthus.sparkfactionapi.api.replay.ReplayTooltipContributor;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Assembles the frozen per-participant hover tooltip and the status/role-chain texts shared with the roster.
 * 组装每名参与者冻结的悬停提示，以及与名单共用的状态与身份链文本。
 */
final class ReplayTooltipBuilder {
    static final String KEY_ROLE = "replay.sparkfactionapi.tooltip.role";
    static final String KEY_FACTION = "replay.sparkfactionapi.tooltip.faction";
    static final String KEY_STATUS = "replay.sparkfactionapi.tooltip.status";
    private static final String KEY_STATUS_PREFIX = "replay.sparkfactionapi.status.";
    private static final String KEY_RESULT_PREFIX = "replay.sparkfactionapi.result.";
    private static final String ARROW = " → ";
    private static final String SEPARATOR = "─".repeat(16);

    private ReplayTooltipBuilder() {
    }

    /** Newline-joined tooltip; the contributor block is omitted when empty. 以换行连接的提示；无贡献内容时省略该段。 */
    static Text tooltip(String name, Text roleChain, Text faction, Text status, List<Text> contributorLines) {
        List<Text> lines = new ArrayList<>(5 + contributorLines.size());
        lines.add(Text.literal(name).formatted(Formatting.WHITE, Formatting.BOLD));
        lines.add(Text.translatable(KEY_ROLE, roleChain).formatted(Formatting.GRAY));
        lines.add(Text.translatable(KEY_FACTION, faction).formatted(Formatting.GRAY));
        lines.add(Text.translatable(KEY_STATUS, status).formatted(Formatting.GRAY));
        if (!contributorLines.isEmpty()) {
            lines.add(Text.literal(SEPARATOR).formatted(Formatting.DARK_GRAY));
            lines.addAll(contributorLines);
        }
        return joinLines(lines);
    }

    static Text joinLines(List<Text> lines) {
        MutableText joined = Text.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                joined.append(Text.literal("\n"));
            }
            joined.append(lines.get(i));
        }
        return joined;
    }

    /**
     * Role names joined by arrows; with {@code withTimes}, each role reached by a change gets a gray time stamp.
     * 以箭头连接的身份名；{@code withTimes} 为真时，由变化得到的身份附带灰色时间戳。
     */
    static Text roleChain(List<ReplayRoleChain.Step> steps, long startTick, boolean withTimes) {
        if (steps.isEmpty()) {
            return ReplayNameRenderer.roleName(null);
        }
        MutableText chain = Text.empty();
        for (int i = 0; i < steps.size(); i++) {
            ReplayRoleChain.Step step = steps.get(i);
            if (i > 0) {
                chain.append(Text.literal(ARROW).formatted(Formatting.GRAY));
            }
            chain.append(ReplayNameRenderer.roleName(step.role()));
            if (withTimes && step.reachedAtTick() != null) {
                chain.append(timeStamp(step.reachedAtTick(), startTick));
            }
        }
        return chain;
    }

    static Text status(ReplayPlayerStatusRules.Status status, long startTick) {
        MutableText text = Text.empty().append(Text.translatable(
                KEY_STATUS_PREFIX + status.presence().name().toLowerCase(Locale.ROOT)
        ).formatted(presenceColor(status.presence())));
        if (status.deathTick() != null) {
            text.append(timeStamp(status.deathTick(), startTick));
        }
        if (status.result() != null) {
            text.append(Text.literal(" · ").formatted(Formatting.GRAY));
            text.append(Text.translatable(
                    KEY_RESULT_PREFIX + (status.result() == ReplayPlayerStatusRules.Result.WIN ? "win" : "lose")
            ).formatted(status.result() == ReplayPlayerStatusRules.Result.WIN ? Formatting.GOLD : Formatting.RED));
        }
        return text;
    }

    /**
     * Faction label using the FactionAPI key convention: the definition's {@code translationKeyPrefix} (legacy Wathe
     * factions are registered as {@code faction.wathe.<path>}), else {@code faction.<ns>.<path>}, else the id path.
     * 使用 FactionAPI 键约定的阵营名：优先定义的 {@code translationKeyPrefix}（Wathe 原版阵营注册为
     * {@code faction.wathe.<path>}），否则 {@code faction.<ns>.<path>}，再否则显示 id 路径。
     */
    static Text faction(Identifier factionId) {
        FactionDefinition definition = SparkFactionApi.getFaction(factionId).orElse(null);
        String key = definition != null
                ? definition.translationKeyPrefix()
                : "faction." + factionId.getNamespace() + "." + factionId.getPath().replace('/', '.');
        MutableText text = Text.translatableWithFallback(key, factionId.getPath());
        return definition != null ? text.withColor(definition.color()) : text;
    }

    /**
     * Runs every contributor in registration order. A contributor that throws is logged and skipped together with
     * any lines it emitted before failing, so one broken add-on never hides the others or breaks the replay.
     * 按注册顺序执行贡献者。抛出异常的贡献者会被记录并整体跳过（包括失败前已输出的行），避免单个附属模组影响其他内容或回放。
     */
    static List<Text> contributorLines(
            ReplayPlayerView view,
            List<Map.Entry<Identifier, ReplayTooltipContributor>> contributors
    ) {
        return runContributors(view, contributors, "tooltip",
                (contributor, sink) -> contributor.contribute(view, sink));
    }

    /**
     * Shared loop for tooltip and badge contributors: each contributor's non-null output is kept only if it returns
     * normally; one that throws is logged as "Replay {kind} contributor ..." and skipped with everything it emitted.
     * 悬停提示与标签贡献者共用的循环：贡献者正常返回时才保留其非空输出；抛异常的贡献者以 "Replay {kind} contributor ..."
     * 记录日志，并连同其已输出的内容一起跳过。
     */
    static <C, T> List<T> runContributors(
            ReplayPlayerView view,
            List<Map.Entry<Identifier, C>> contributors,
            String kind,
            BiConsumer<C, Consumer<T>> contribute
    ) {
        List<T> out = new ArrayList<>();
        for (Map.Entry<Identifier, C> entry : contributors) {
            List<T> own = new ArrayList<>();
            try {
                contribute.accept(entry.getValue(), item -> {
                    if (item != null) {
                        own.add(item);
                    }
                });
            } catch (RuntimeException | LinkageError e) {
                SparkFactionApiMod.LOGGER.error(
                        "Replay {} contributor {} failed for player {}; skipping it",
                        kind,
                        entry.getKey(),
                        view.uuid(),
                        e
                );
                continue;
            }
            out.addAll(own);
        }
        return out;
    }

    static Text timeStamp(long tick, long startTick) {
        return Text.literal(" [" + ReplayGenerator.formatTime(tick, startTick) + "]").formatted(Formatting.GRAY);
    }

    private static Formatting presenceColor(ReplayPlayerStatusRules.Presence presence) {
        return switch (presence) {
            case ALIVE -> Formatting.GREEN;
            case DEAD -> Formatting.RED;
            case LEFT -> Formatting.GRAY;
        };
    }
}
