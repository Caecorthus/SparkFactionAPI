package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the most recent replay so {@code /replay} can reopen it: the exact chat lines plus, when it could be built and
 * fits the payload budget, the screen snapshot and the wall-clock end time used to stamp "ended N seconds ago".
 * Wathe never exposes the sent lines, so the chat framing below mirrors {@code ReplayGenerator.sendReplayToPlayer}
 * (Wathe 1.5.6) line for line; update it together with the pinned Wathe jar.
 * 保存最近一次回放供 {@code /replay} 重新打开：完整聊天行，以及（能构建且未超出数据包预算时）界面快照与用于计算
 * “多久前结束”的结束时刻。Wathe 不暴露已发送内容，因此聊天框架逐行复刻 {@code ReplayGenerator.sendReplayToPlayer}
 * （Wathe 1.5.6）；升级 Wathe jar 时需同步核对。
 */
public final class ReplayArchive {
    private static final String RULE = "═".repeat(40);
    private static final Entry EMPTY = new Entry(List.of(), null, 0, 0L);

    private static volatile Entry latest = EMPTY;

    private ReplayArchive() {
    }

    /**
     * One stored replay; {@code snapshot} is null when it failed to build or exceeded the payload budget.
     * 一份已存回放；构建失败或超出数据包预算时 {@code snapshot} 为 null。
     */
    public record Entry(List<Text> chat, @Nullable ReplaySnapshot snapshot, int snapshotBytes, long endedAtMillis) {
        public Entry {
            chat = List.copyOf(chat);
        }

        public boolean isEmpty() {
            return chat.isEmpty() && snapshot == null;
        }

        /** Whole seconds since the end, clamped to {@code [0, Integer.MAX_VALUE]}. 距结束的整秒数，限制在非负 int 范围。 */
        public int endedSecondsAgo(long nowMillis) {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (nowMillis - endedAtMillis) / 1000L));
        }
    }

    public static void store(List<Text> chat, @Nullable ReplaySnapshot snapshot, int snapshotBytes, long endedAtMillis) {
        latest = new Entry(chat, snapshot, snapshotBytes, endedAtMillis);
    }

    public static Entry current() {
        return latest;
    }

    /** Chat lines of the latest replay, or an empty list when none is stored. 最近一次回放的聊天行；没有时为空列表。 */
    public static List<Text> latest() {
        return latest.chat();
    }

    public static void clear() {
        latest = EMPTY;
    }

    /**
     * Header, blank, body (roster + event lines), blank, footer. The live reopen hint is not archived, so a resend
     * does not tell the player to run the command they just ran.
     * 标题、空行、正文（名单与事件行）、空行、结尾。实时发送的重看提示不存档，重发时不会再提示玩家执行刚执行过的命令。
     */
    static List<Text> frame(List<Text> body) {
        List<Text> lines = new ArrayList<>(body.size() + 6);
        lines.add(Text.literal(RULE).formatted(Formatting.DARK_GRAY));
        lines.add(Text.translatable("replay.title").formatted(Formatting.GOLD, Formatting.BOLD));
        lines.add(Text.empty());
        lines.addAll(body);
        lines.add(Text.empty());
        lines.add(Text.translatable("replay.footer").formatted(Formatting.GRAY));
        lines.add(Text.literal(RULE).formatted(Formatting.DARK_GRAY));
        return lines;
    }
}
