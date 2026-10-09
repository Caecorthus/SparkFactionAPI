package dev.caecorthus.sparkfactionapi.impl.record;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-match coin income totals for {@code sparkfactionapi:income}. Every Wathe {@code PlayerShopComponent#addToBalance}
 * call adds its actual balance change (after − before) when it is positive; a zero or negative change adds nothing.
 * Totals belong to one match id: a different match id or {@link #reset} forgets the previous match, so a round that
 * never reached {@code endMatch} cannot leak into the next one, and {@link #drain} hands a match's totals out once.
 * 为 {@code sparkfactionapi:income} 按对局累计金币收入。每次 Wathe {@code PlayerShopComponent#addToBalance} 调用的实际
 * 余额变化（调用后 − 调用前）为正时计入；为零或为负时不计。累计值属于单个对局标识：对局标识变化或 {@link #reset} 会
 * 清空上一局，因此未走到 {@code endMatch} 的回合不会漏进下一局；{@link #drain} 只交出一次该局的累计值。
 */
public final class IncomeTotals {
    private @Nullable UUID matchId;
    // Insertion order = each player's first income, so the written events have a stable order.
    // 插入顺序即每名玩家首次入账的顺序，使写出的事件顺序稳定。
    private final Map<UUID, Long> totals = new LinkedHashMap<>();

    /** Income of one call: the positive part of after − before. 单次调用的收入：调用后 − 调用前的正值部分。 */
    public static long actualIncome(int balanceBefore, int balanceAfter) {
        return Math.max(0L, (long) balanceAfter - balanceBefore);
    }

    public synchronized void add(UUID match, UUID player, long income) {
        Objects.requireNonNull(player, "player");
        enter(match);
        if (income > 0) {
            totals.merge(player, income, IncomeTotals::saturatedSum);
        }
    }

    /**
     * Totals of {@code match} (each positive, capped at {@link Integer#MAX_VALUE}) in first-income order, then
     * forgets them. Totals of any other match are dropped, not returned.
     * 返回 {@code match} 的累计值（均为正，上限 {@link Integer#MAX_VALUE}，按首次入账顺序）并清空。其他对局的累计值
     * 直接丢弃，不会返回。
     */
    public synchronized Map<UUID, Integer> drain(UUID match) {
        enter(match);
        Map<UUID, Integer> drained = new LinkedHashMap<>();
        totals.forEach((player, total) -> drained.put(player, (int) Math.min(Integer.MAX_VALUE, total)));
        totals.clear();
        return Collections.unmodifiableMap(drained);
    }

    /** Forgets every total; the next call starts a fresh match. 清空全部累计值，下次调用从新对局开始。 */
    public synchronized void reset() {
        matchId = null;
        totals.clear();
    }

    private void enter(UUID match) {
        Objects.requireNonNull(match, "match");
        if (!match.equals(matchId)) {
            matchId = match;
            totals.clear();
        }
    }

    private static long saturatedSum(long a, long b) {
        long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }
}
