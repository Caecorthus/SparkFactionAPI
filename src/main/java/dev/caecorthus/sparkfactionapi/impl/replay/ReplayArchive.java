package dev.caecorthus.sparkfactionapi.impl.replay;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the exact chat lines of the most recent replay so {@code /replay} can resend them.
 * Wathe never exposes the sent lines, so the framing below mirrors {@code ReplayGenerator.sendReplayToPlayer}
 * (Wathe 1.5.6) line for line; update it together with the pinned Wathe jar.
 * 保存最近一次回放的完整聊天行，供 {@code /replay} 重发。Wathe 不暴露已发送内容，因此下方框架逐行复刻
 * {@code ReplayGenerator.sendReplayToPlayer}（Wathe 1.5.6）；升级 Wathe jar 时需同步核对。
 */
public final class ReplayArchive {
    private static final String RULE = "═".repeat(40);

    private static volatile List<Text> latest = List.of();

    private ReplayArchive() {
    }

    public static void store(List<Text> lines) {
        latest = List.copyOf(lines);
    }

    /** Lines of the latest replay, or an empty list when none is stored. 最近一次回放的行；没有时为空列表。 */
    public static List<Text> latest() {
        return latest;
    }

    public static void clear() {
        latest = List.of();
    }

    /**
     * Header, blank, body (roster + event lines), blank, footer, then the reopen hint appended by FactionAPI.
     * 标题、空行、正文（名单与事件行）、空行、结尾，最后是 FactionAPI 追加的重看提示。
     */
    static List<Text> frame(List<Text> body, Text hint) {
        List<Text> lines = new ArrayList<>(body.size() + 7);
        lines.add(Text.literal(RULE).formatted(Formatting.DARK_GRAY));
        lines.add(Text.translatable("replay.title").formatted(Formatting.GOLD, Formatting.BOLD));
        lines.add(Text.empty());
        lines.addAll(body);
        lines.add(Text.empty());
        lines.add(Text.translatable("replay.footer").formatted(Formatting.GRAY));
        lines.add(Text.literal(RULE).formatted(Formatting.DARK_GRAY));
        lines.add(hint);
        return lines;
    }
}
