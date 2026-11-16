package dev.caecorthus.sparkfactionapi.impl.thirdfaction;

import dev.caecorthus.sparkfactionapi.api.FactionCategory;
import dev.doctor4t.wathe.api.Faction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntUnaryOperator;

/**
 * Pure decisions of the third-faction draw (owner rules 2026-11-16); {@link ThirdFactionDraw} only gathers Minecraft
 * state and applies the plan. Generic over the faction key {@code K} and the role type {@code R} so it is tested
 * without a world.
 * 第三方阵营抽取的纯决策（所有者规则 2026-11-16）；{@link ThirdFactionDraw} 只收集 Minecraft 状态并执行计划。
 * 对阵营键 {@code K} 与角色类型 {@code R} 泛型化，便于在没有世界的情况下测试。
 */
public final class ThirdFactionDrawRules {
    public static final int DEFAULT_DIVIDEND = 10;
    /** Same floor as Wathe's vigilante/neutral dividend argument. 与 Wathe 义警/中立除数参数的下限一致。 */
    public static final int MIN_DIVIDEND = 3;
    public static final int DEFAULT_MIN_PLAYERS = 18;
    /** 0 means "no minimum". 0 表示没有最低人数。 */
    public static final int MIN_MIN_PLAYERS = 0;

    private ThirdFactionDrawRules() {
    }

    /** Why a plan deals what it deals. 计划为何如此发放。 */
    public enum Outcome {
        /** Fewer players than the minimum, or the cap floors to 0. 人数低于最低要求，或上限向下取整为 0。 */
        NO_CAP,
        /** Present members of two or more third factions: deal nothing more. 已有两个及以上第三方阵营的成员：不再发放。 */
        CONTESTED,
        /** Exactly one third faction is present: it is the round's faction. 恰有一个第三方阵营在场：它就是本局阵营。 */
        PRESENT,
        /** No third faction was present and one lineup was picked at random. 没有在场阵营，随机抽中了一个阵容。 */
        DRAWN,
        /** No third faction was present and no lineup can be drawn. 没有在场阵营，也没有可抽取的阵容。 */
        NO_CANDIDATE
    }

    /**
     * The round's plan. {@code faction} is null for NO_CAP, CONTESTED and NO_CANDIDATE; {@code toDeal} is already
     * truncated to the remaining slots and the available seats.
     * 本局计划。NO_CAP、CONTESTED 与 NO_CANDIDATE 时 {@code faction} 为 null；{@code toDeal} 已按剩余名额和可用座位截断。
     */
    public record Plan<K, R>(@Nullable K faction, List<R> toDeal, Outcome outcome) {
        public Plan {
            toDeal = List.copyOf(toDeal);
        }
    }

    /**
     * Category of a Wathe {@code Faction} constant: null and NONE are NONE, the other built-ins map 1:1, any other
     * constant is THIRD_FACTION. Switch-free, so an extended {@code Faction} needs no switch map.
     * Wathe {@code Faction} 常量的分类：null 与 NONE 为 NONE，其余内置常量一一对应，其它任何常量为 THIRD_FACTION。
     * 不使用 switch，因此扩展后的 {@code Faction} 不需要 switch 映射表。
     */
    public static FactionCategory categoryOf(@Nullable Faction faction) {
        if (faction == null || faction == Faction.NONE) {
            return FactionCategory.NONE;
        }
        if (faction == Faction.CIVILIAN) {
            return FactionCategory.CIVILIAN;
        }
        if (faction == Faction.NEUTRAL) {
            return FactionCategory.NEUTRAL;
        }
        if (faction == Faction.KILLER) {
            return FactionCategory.KILLER;
        }
        return FactionCategory.THIRD_FACTION;
    }

    /**
     * Member cap of the round: 0 below {@code minPlayers}, otherwise {@code floor(totalPlayers / dividend)}.
     * 本局成员上限：低于 {@code minPlayers} 时为 0，否则为 {@code floor(totalPlayers / dividend)}。
     */
    public static int memberCap(int totalPlayers, int dividend, int minPlayers) {
        if (totalPlayers <= 0 || dividend <= 0 || totalPlayers < minPlayers) {
            return 0;
        }
        return totalPlayers / dividend;
    }

    /**
     * Plans the draw.
     * <ol>
     *     <li>{@code memberCap < 1}: NO_CAP.</li>
     *     <li>{@code present} holds two or more factions: CONTESTED.</li>
     *     <li>Exactly one: PRESENT; its lineup (when it is in {@code lineupFactions}) is asked with its present members
     *     and fills up to the cap, present members included.</li>
     *     <li>None: every faction of {@code lineupFactions} (in that order) whose answer for no present members is
     *     non-empty is a candidate; one is picked with {@code randomBelow.applyAsInt(candidates.size())}: DRAWN, or
     *     NO_CANDIDATE when there is none. The probe answer is the one dealt.</li>
     * </ol>
     * The dealt list skips null entries and is truncated to {@code memberCap - present members} and to
     * {@code availableSeats}.
     * 规划抽取：上限小于 1 时为 NO_CAP；在场阵营不少于两个时为 CONTESTED；恰有一个时为 PRESENT，若它有阵容则以其在场成员
     * 询问，并补足到上限（在场成员计入上限）；没有在场阵营时，按 {@code lineupFactions} 顺序把“无在场成员时回答非空”的阵营
     * 列为候选，用 {@code randomBelow.applyAsInt(候选数)} 均匀抽取一个（DRAWN），没有候选时为 NO_CANDIDATE；发放的就是探测
     * 时的回答。发放列表跳过 null，并截断到“上限减在场成员数”与 {@code availableSeats}。
     *
     * @param present        present members per third faction, in first-seen order 每个第三方阵营的在场成员（按首次出现顺序）
     * @param lineupFactions factions that have a lineup, in declared order 有阵容的阵营（按声明顺序）
     * @param rolesToDeal    (faction, present members) → the lineup's answer (factions of {@code lineupFactions} only)
     *                       （阵营，在场成员）→ 阵容的回答（只对 {@code lineupFactions} 中的阵营调用）
     * @param randomBelow    n → a uniform int in {@code [0, n)} n → {@code [0, n)} 内的均匀整数
     */
    public static <K, R> Plan<K, R> plan(
            int memberCap,
            Map<K, List<R>> present,
            List<K> lineupFactions,
            BiFunction<K, List<R>, List<R>> rolesToDeal,
            int availableSeats,
            IntUnaryOperator randomBelow
    ) {
        if (memberCap < 1) {
            return new Plan<>(null, List.of(), Outcome.NO_CAP);
        }
        if (present.size() >= 2) {
            return new Plan<>(null, List.of(), Outcome.CONTESTED);
        }
        if (present.size() == 1) {
            Map.Entry<K, List<R>> only = present.entrySet().iterator().next();
            K faction = only.getKey();
            List<R> members = only.getValue();
            if (!lineupFactions.contains(faction)) {
                return new Plan<>(faction, List.of(), Outcome.PRESENT);
            }
            List<R> proposal = withoutNulls(rolesToDeal.apply(faction, List.copyOf(members)));
            return new Plan<>(faction, truncate(proposal, memberCap - members.size(), availableSeats), Outcome.PRESENT);
        }

        List<K> candidates = new ArrayList<>();
        List<List<R>> proposals = new ArrayList<>();
        for (K faction : lineupFactions) {
            List<R> proposal = withoutNulls(rolesToDeal.apply(faction, List.of()));
            if (!proposal.isEmpty()) {
                candidates.add(faction);
                proposals.add(proposal);
            }
        }
        if (candidates.isEmpty()) {
            return new Plan<>(null, List.of(), Outcome.NO_CANDIDATE);
        }
        int index = candidates.size() == 1 ? 0 : randomBelow.applyAsInt(candidates.size());
        return new Plan<>(candidates.get(index), truncate(proposals.get(index), memberCap, availableSeats), Outcome.DRAWN);
    }

    /**
     * First {@code min(proposal.size(), remainingSlots, availableSeats)} entries (never negative).
     * 取前 {@code min(proposal.size(), remainingSlots, availableSeats)} 个（不会为负）。
     */
    public static <R> List<R> truncate(List<R> proposal, int remainingSlots, int availableSeats) {
        int count = Math.max(0, Math.min(proposal.size(), Math.min(remainingSlots, availableSeats)));
        return List.copyOf(proposal.subList(0, count));
    }

    /**
     * Whether Wathe's neutral pass may still read a {@code dealtInWatheNeutralPool} faction as NEUTRAL: always when no
     * draw state is set ({@code claims == null}) or no third faction is present, and otherwise only for the single
     * present faction itself. Keeps "at most one third faction per round" for pool-dealt native factions.
     * Wathe 中立分配是否仍可把 {@code dealtInWatheNeutralPool} 阵营读作 NEUTRAL：没有抽取状态（{@code claims == null}）
     * 或没有在场第三方阵营时总是可以，否则只对唯一在场的那个阵营本身可以。为进入中立池的原生阵营保持“每局最多一个
     * 第三方阵营”。
     */
    public static <K> boolean neutralPoolOpen(@Nullable Collection<K> claims, K faction) {
        return claims == null || claims.isEmpty() || (claims.size() == 1 && claims.contains(faction));
    }

    private static <R> List<R> withoutNulls(@Nullable List<R> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        List<R> result = new ArrayList<>(roles.size());
        for (R role : roles) {
            if (role != null) {
                result.add(role);
            }
        }
        return result;
    }
}
