package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.api.Role;

import java.util.List;

/**
 * Lineup of a third faction, registered with {@link SparkFactionApi#registerThirdFactionLineup}. SparkFactionAPI's
 * third-faction draw runs once per Wathe murder round, after the vigilante pass and before Wathe's neutral pass, and
 * draws at most one third faction per round.
 * 第三方阵营的阵容，通过 {@link SparkFactionApi#registerThirdFactionLineup} 注册。SparkFactionAPI 的第三方阵营抽取在每局
 * Wathe 谋杀模式中执行一次，位于义警分配之后、Wathe 中立分配之前，每局最多抽出一个第三方阵营。
 *
 * @since 2.0.0
 */
@FunctionalInterface
public interface ThirdFactionLineup {
    /**
     * Pure (no side effects): roles to ADD, in deal order, when this faction is the round's third faction.
     * SparkFactionAPI deals at most {@code context.remainingSlots()} of them, each to a distinct random unassigned
     * seat; every dealt role takes one slot whatever its own faction. An empty list means "cannot be drawn this round"
     * (for example the lead role is disabled) and removes the faction from the random pick. Each considered faction is
     * asked once per round: with no present third faction every lineup is probed with empty present members and the
     * picked one's answer is dealt as is; with one present faction only that lineup is asked. Null and
     * {@code WatheRoles.NO_ROLE} entries are skipped; a thrown exception or linkage error is logged and treated as an
     * empty list.
     * 纯函数（无副作用）：当本阵营成为本局第三方阵营时要追加的角色，按发放顺序排列。SparkFactionAPI 最多发放
     * {@code context.remainingSlots()} 个，每个发给不同的随机未分配玩家；无论角色自身阵营如何，每个已发放角色都占一个
     * 名额。空列表表示“本局无法被抽中”（例如主角色被禁用），该阵营会退出随机抽取。每个被考虑的阵营每局只被询问一次：
     * 没有在场第三方阵营时，每个阵容都以空的在场成员被探测，抽中者的回答原样发放；恰有一个在场阵营时只询问该阵容。null 与 {@code WatheRoles.NO_ROLE} 条目会被跳过；抛出的异常或链接错误会记录日志并视为空列表。
     */
    List<Role> rolesToDeal(ThirdFactionDrawContext context);
}
