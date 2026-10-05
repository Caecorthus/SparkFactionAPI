package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.command.replay.ReplayCommand;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Short end-of-match chat summary sent instead of the full chat replay to players whose client can open the replay
 * screen. It only needs the session, because Wathe sends chat before the snapshot is built; the buttons run commands
 * that read the archive, which exists by the time they are clicked.
 * 局末聊天摘要：发给能打开回放界面的玩家，替代完整聊天回放。Wathe 发送聊天早于快照构建，因此摘要只依赖会话；
 * 按钮执行的命令读取存档，点击时存档已经存在。
 */
public final class ReplaySummary {
    static final String KEY_TITLE = "replay.sparkfactionapi.summary.title";
    static final String KEY_STATS = "replay.sparkfactionapi.summary.stats";
    static final String KEY_OPEN = "replay.sparkfactionapi.summary.open";
    static final String KEY_CHAT = "replay.sparkfactionapi.summary.chat";
    static final String OPEN_COMMAND = ReplayPresentation.REOPEN_COMMAND;
    static final String CHAT_COMMAND = ReplayPresentation.REOPEN_COMMAND + " " + ReplayCommand.CHAT_LITERAL;
    private static final String RULE = "═".repeat(40);

    private ReplaySummary() {
    }

    public record Stats(int players, int deaths, int conversions, int durationSeconds) {
    }

    /**
     * Participants, DEAD participants, recorded role changes, and whole seconds from start to end.
     * 参与者数、死亡参与者数、记录到的身份转化次数，以及开局到结束的整秒数。
     */
    public static Stats stats(
            ReplayRoleTimeline timeline,
            Collection<ReplayPlayerStatusRules.Status> statuses,
            long startTick,
            long endTick
    ) {
        int conversions = 0;
        for (UUID uuid : timeline.players()) {
            conversions += timeline.changes(uuid).size();
        }
        int deaths = 0;
        for (ReplayPlayerStatusRules.Status status : statuses) {
            if (status.presence() == ReplayPlayerStatusRules.Presence.DEAD) {
                deaths++;
            }
        }
        return new Stats(timeline.players().size(), deaths, conversions, secondsBetween(startTick, endTick));
    }

    /** Whole seconds from {@code startTick}, never negative. 自 {@code startTick} 起的整秒数，不为负。 */
    static int secondsBetween(long startTick, long tick) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (tick - startTick) / 20L));
    }

    static String clock(int seconds) {
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    /** Rule, title, stats, buttons, rule. 分隔线、标题、统计、按钮、分隔线。 */
    public static List<Text> lines(Stats stats) {
        Text rule = Text.literal(RULE).formatted(Formatting.DARK_GRAY);
        return List.of(
                rule,
                Text.translatable(KEY_TITLE).formatted(Formatting.GOLD, Formatting.BOLD),
                Text.translatable(KEY_STATS, stats.players(), stats.deaths(), stats.conversions(), clock(stats.durationSeconds()))
                        .formatted(Formatting.GRAY),
                Text.empty()
                        .append(button(KEY_OPEN, Formatting.GREEN, OPEN_COMMAND))
                        .append(Text.literal(" "))
                        .append(button(KEY_CHAT, Formatting.GRAY, CHAT_COMMAND)),
                rule
        );
    }

    private static MutableText button(String key, Formatting color, String command) {
        return Text.translatable(key).formatted(color).styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable(key + ".hover"))));
    }
}
