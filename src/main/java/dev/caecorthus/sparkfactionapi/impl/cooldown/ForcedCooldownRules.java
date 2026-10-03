package dev.caecorthus.sparkfactionapi.impl.cooldown;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Pure arithmetic and ordering rules behind {@code ForcedCooldowns}; free of Minecraft state so they stay testable.
 * {@code ForcedCooldowns} 背后的纯算术与排序规则；不依赖 Minecraft 状态，便于测试。
 */
final class ForcedCooldownRules {
    private ForcedCooldownRules() {
    }

    /**
     * Monotonic floor: the new remaining value, or empty when it would not exceed the current one.
     * 单调下限：返回新的剩余值；若不大于当前值则返回空（不写入）。
     */
    static OptionalInt raiseTarget(int currentRemaining, int ticks) {
        return ticks > Math.max(0, currentRemaining) ? OptionalInt.of(ticks) : OptionalInt.empty();
    }

    /**
     * Monotonic extension: remaining + ticks, saturating at {@link Integer#MAX_VALUE}; non-positive ticks never write.
     * 单调延长：剩余 + ticks，在 {@link Integer#MAX_VALUE} 处饱和；非正数不写入。
     */
    static OptionalInt extendTarget(int currentRemaining, int ticks) {
        if (ticks <= 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(saturatingAdd(Math.max(0, currentRemaining), ticks));
    }

    static int saturatingAdd(int base, int ticks) {
        long sum = (long) base + ticks;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, sum));
    }

    /** Remaining ticks of a vanilla entry, floored at zero. / 原版条目的剩余 tick，下限为 0。 */
    static int remaining(int endTick, int currentTick) {
        long remaining = (long) endTick - currentTick;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
    }

    /**
     * Distinct, non-exempt items over the inventory sections in the given order (main, offhand, armor), skipping
     * empty stacks. Items whose cooldown is ready are kept: readiness is not a filter.
     * 按给定分区顺序（主背包、副手、盔甲）去重收集未豁免的物品，跳过空堆；已就绪的物品同样保留。
     */
    static <S, I> List<I> distinctCarriedItems(
            List<? extends List<? extends S>> sections,
            Predicate<? super S> isEmpty,
            Function<? super S, ? extends I> itemOf,
            Predicate<? super I> exempt
    ) {
        Set<I> seen = new HashSet<>();
        List<I> items = new ArrayList<>();
        for (List<? extends S> section : sections) {
            for (S stack : section) {
                if (stack == null || isEmpty.test(stack)) {
                    continue;
                }
                I item = itemOf.apply(stack);
                // Each distinct item is classified once; exempt items stay in "seen" so they are never re-tested.
                // 每种物品只判定一次；豁免物品也记入 seen，避免重复判定。
                if (item != null && seen.add(item) && !exempt.test(item)) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    /** True when some non-empty stack in the sections holds the item. / 任一分区中存在该物品的非空堆时为真。 */
    static <S, I> boolean carries(
            List<? extends List<? extends S>> sections,
            Predicate<? super S> isEmpty,
            Function<? super S, ? extends I> itemOf,
            I item
    ) {
        for (List<? extends S> section : sections) {
            for (S stack : section) {
                if (stack != null && !isEmpty.test(stack) && Objects.equals(itemOf.apply(stack), item)) {
                    return true;
                }
            }
        }
        return false;
    }
}
