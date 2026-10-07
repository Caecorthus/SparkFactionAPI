package dev.caecorthus.sparkfactionapi.impl.cooldown;

import com.google.common.collect.MapMaker;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * Remembers which part of a live vanilla item-cooldown entry {@code ForcedCooldowns} forced, in that entry's
 * {@code ItemCooldownManager} tick space. A raise is a floor that runs from its write (an absolute end tick); an
 * extend appends ticks after everything already there (a trailing amount, served only once the time before it has
 * run). Keys are the entry objects themselves, held weakly and compared by identity, so a record dies with its
 * entry: any later {@code set} (new entry) or {@code remove} (admin {@code clearCooldown}, Wathe reset, expiry) drops
 * the forced part and nothing stale is ever restored. Free of Minecraft types so the arithmetic stays testable.
 * 记录 {@code ForcedCooldowns} 在某个实时原版物品冷却条目上强制施加的部分（该条目所属 {@code ItemCooldownManager} 的刻）。
 * raise 是自写入起计时的下限（绝对结束刻）；extend 把时长追加在已有冷却之后（尾部时长，前面的时间走完后才开始消耗）。
 * 键即条目对象本身，弱引用且按身份比较，记录随条目一同消亡：之后任何 {@code set}（新条目）或 {@code remove}
 * （管理员 clearCooldown、Wathe 重置、到期）都会丢弃强制部分，绝不恢复过期记录。不依赖 Minecraft 类型，便于测试算术。
 */
final class ForcedItemLedger {
    private static final int NO_FLOOR = Integer.MIN_VALUE;

    private final ConcurrentMap<Object, Forced> forced = new MapMaker().weakKeys().makeMap();

    /**
     * A forced floor of {@code ticks} from {@code tick}; recorded even when a longer natural cooldown already covers
     * it, and never moves an existing, later-ending floor back. Non-positive ticks are ignored.
     * 自 {@code tick} 起 {@code ticks} 的强制下限；即使更长的自然冷却已覆盖它也会记录，且不会提前已有的更晚结束刻。
     * 非正数忽略。
     */
    void raised(Object entry, int tick, int ticks) {
        Objects.requireNonNull(entry, "entry");
        if (ticks <= 0) {
            return;
        }
        int end = ForcedCooldownRules.saturatingAdd(tick, ticks);
        forced.merge(entry, new Forced(end, 0),
                (old, added) -> new Forced(Math.max(old.floorEnd(), end), old.trailingTicks()));
    }

    /**
     * A forced extension appended after the {@code remainingBefore} ticks the entry had left (0 when it was ready):
     * the still-unserved trailing forced ticks grow by {@code ticks}; the natural part is never counted as forced.
     * Non-positive ticks are ignored.
     * 追加在条目原剩余 {@code remainingBefore} tick（已就绪时为 0）之后的强制延长：尚未消耗的尾部强制时长增加
     * {@code ticks}；自然部分永不计为强制。非正数忽略。
     */
    void extended(Object entry, int remainingBefore, int ticks) {
        Objects.requireNonNull(entry, "entry");
        if (ticks <= 0) {
            return;
        }
        forced.compute(entry, (key, old) -> old == null
                ? new Forced(NO_FLOOR, ticks)
                : new Forced(old.floorEnd(), ForcedCooldownRules.saturatingAdd(
                        Math.min(old.trailingTicks(), Math.max(0, remainingBefore)), ticks)));
    }

    /**
     * Forced ticks still covering the entry: the unexpired floor plus the trailing extension (the part of it the
     * entry still has), overlap counted once and never more than the entry itself has left; 0 for a {@code null}
     * entry or one without a forced part.
     * 仍覆盖该条目的强制 tick：未到期的下限加上尾部延长（条目仍保有的部分），重叠只计一次，且绝不超过条目自身剩余；
     * 条目为 {@code null} 或无强制部分时为 0。
     */
    int keptTicks(Object entry, int entryEndTick, int tick) {
        if (entry == null) {
            return 0;
        }
        Forced part = forced.get(entry);
        if (part == null) {
            return 0;
        }
        int left = ForcedCooldownRules.remaining(entryEndTick, tick);
        // The trailing part is the entry's last ticks, so floor + trailing > left means they overlap: all is forced.
        // 尾部即条目最后的若干 tick，因此下限 + 尾部 > 剩余 时二者重叠：全部为强制。
        long sum = (long) Math.min(left, ForcedCooldownRules.remaining(part.floorEnd(), tick))
                + Math.min(left, part.trailingTicks());
        return (int) Math.min(left, sum);
    }

    private record Forced(int floorEnd, int trailingTicks) {
    }
}
