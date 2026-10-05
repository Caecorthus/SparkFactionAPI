package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Pure size guard for the replay snapshot. When the encoded snapshot is over budget, whole low-value categories are
 * dropped in a fixed order (OTHER, then ITEM); if it still does not fit, the caller skips the snapshot and players
 * fall back to the chat replay.
 * 回放快照的纯尺寸保护。编码后超出预算时，按固定顺序整类丢弃低价值行（先 OTHER，再 ITEM）；仍放不下时由调用方跳过快照，
 * 玩家改用聊天回放。
 */
public final class ReplaySnapshotTrimRules {
    /** Dropped first to last. 由先到后依次丢弃。 */
    static final List<ReplaySnapshot.Category> DROP_ORDER =
            List.of(ReplaySnapshot.Category.OTHER, ReplaySnapshot.Category.ITEM);

    private ReplaySnapshotTrimRules() {
    }

    /**
     * @param dropped categories removed to fit, in drop order; 为放入预算而移除的分类，按丢弃顺序
     */
    public record Fit(List<ReplaySnapshot.Line> lines, List<ReplaySnapshot.Category> dropped, long encodedSize) {
        public Fit {
            lines = List.copyOf(lines);
            dropped = List.copyOf(dropped);
        }
    }

    /**
     * Lines that fit both {@code maxLines} and {@code maxBytes} as measured by {@code encodedSize}, or null.
     * {@code encodedSize} is called at most {@code DROP_ORDER.size() + 1} times.
     * 返回同时满足 {@code maxLines} 与 {@code maxBytes}（由 {@code encodedSize} 计量）的行；都放不下时返回 null。
     */
    public static @Nullable Fit fit(
            List<ReplaySnapshot.Line> lines,
            ToLongFunction<List<ReplaySnapshot.Line>> encodedSize,
            long maxBytes,
            int maxLines
    ) {
        List<ReplaySnapshot.Line> current = List.copyOf(lines);
        List<ReplaySnapshot.Category> dropped = new ArrayList<>();
        for (int step = 0; ; step++) {
            if (current.size() <= maxLines) {
                long size = encodedSize.applyAsLong(current);
                if (size <= maxBytes) {
                    return new Fit(current, dropped, size);
                }
            }
            if (step >= DROP_ORDER.size()) {
                return null;
            }
            ReplaySnapshot.Category category = DROP_ORDER.get(step);
            current = current.stream().filter(line -> line.category() != category).toList();
            dropped.add(category);
        }
    }
}
