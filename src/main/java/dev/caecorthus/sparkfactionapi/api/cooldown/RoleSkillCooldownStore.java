package dev.caecorthus.sparkfactionapi.api.cooldown;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.OptionalInt;

/**
 * One role-skill cooldown counter that downstream mods expose so other features can force it. The owning mod
 * registers it once during initialization; SparkFactionAPI only enumerates and writes through it, on the server
 * thread. Implementations own syncing and any side state (for example a deferred cooldown that starts when an
 * active window ends): override {@link #raiseTo} and {@link #extendBy} when a plain read-then-write would lose the
 * penalty.
 * 下游模组暴露的一项职业技能冷却计数，供其他功能强制施加。由所属模组在初始化时注册一次；SparkFactionAPI 只在服务端线程上
 * 枚举并通过它写入。实现方负责同步及附带状态（例如主动窗口结束时才开始的延后冷却）：若简单的先读后写会丢失惩罚，
 * 请覆写 {@link #raiseTo} 与 {@link #extendBy}。
 */
public interface RoleSkillCooldownStore {
    /** Stable unique id, e.g. {@code sparkwitch:witch_skill}. / 稳定且唯一的 id。 */
    Identifier id();

    /** True when this player currently uses this counter. / 该玩家当前是否使用此计数。 */
    boolean appliesTo(ServerPlayerEntity player);

    /** Remaining ticks; values below zero are treated as zero. / 剩余 tick；负数视为 0。 */
    int remainingTicks(ServerPlayerEntity player);

    /** The full cooldown of this skill for this player when known. / 已知时为该玩家此技能的完整冷却。 */
    OptionalInt nominalTicks(ServerPlayerEntity player);

    /**
     * Writes the exact remaining ticks and syncs them to the owner. SparkFactionAPI never calls it with a value
     * below the current remaining time.
     * 精确写入剩余 tick 并同步给本人。SparkFactionAPI 不会用低于当前剩余值的数调用它。
     */
    void setRemainingTicks(ServerPlayerEntity player, int ticks);

    /**
     * False while forcing would break the owner's own flow (for example a release press gated by the same counter).
     * Such a store is left out of {@link ForcedCooldowns#slots} and is never written.
     * 当强制会破坏所属流程（例如同一计数还拦着"放下"操作）时返回 false；此时该存储不出现在 slots 中，也不会被写入。
     */
    default boolean mayForce(ServerPlayerEntity player) {
        return true;
    }

    /**
     * Monotonic floor: afterwards at least {@code ticks} remain; never shortens. Returns true when it wrote.
     * 单调下限：之后至少剩余 {@code ticks}；绝不缩短。写入时返回 true。
     */
    default boolean raiseTo(ServerPlayerEntity player, int ticks) {
        int current = Math.max(0, remainingTicks(player));
        if (ticks <= current) {
            return false;
        }
        setRemainingTicks(player, ticks);
        return true;
    }

    /**
     * Monotonic extension: adds {@code ticks} to the remaining time; a ready skill starts a fresh cooldown of
     * {@code ticks}. Non-positive amounts are ignored. Returns true when it wrote.
     * 单调延长：在剩余时间上加 {@code ticks}；已就绪的技能从 {@code ticks} 开始新冷却。非正数忽略。写入时返回 true。
     */
    default boolean extendBy(ServerPlayerEntity player, int ticks) {
        if (ticks <= 0) {
            return false;
        }
        int current = Math.max(0, remainingTicks(player));
        long sum = (long) current + ticks;
        setRemainingTicks(player, (int) Math.min(Integer.MAX_VALUE, sum));
        return true;
    }
}
