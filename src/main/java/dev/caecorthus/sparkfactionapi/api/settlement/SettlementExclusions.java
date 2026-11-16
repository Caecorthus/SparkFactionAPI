package dev.caecorthus.sparkfactionapi.api.settlement;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.mixin.settlement.GameWorldComponentWorldAccessor;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Identifier-based registry of roles that never count as a living player for round settlement in any mod: killer win
 * checks, last-stand and final-moment thresholds, and neutral "last one alive" wins. Pure classification: it changes
 * no win rule, event, or alive state by itself; each settlement point asks it and skips excluded players.
 * 基于身份标识符的结算排除注册表：已注册身份在任何模组的回合结算中都不算存活玩家，包括杀手胜利判定、背水一战与
 * 终局时刻的门槛，以及中立“最后一人”胜利。仅做分类：自身不改变任何胜利规则、事件或存活状态；由各结算点查询并跳过被排除的玩家。
 * There are no defaults; role owners register during mod initialization, and reads happen on the server thread
 * during a round. A role that is excluded stays excluded for the process lifetime (no removal).
 * 没有默认成员；身份所属模组在初始化时注册，读取发生在对局中的服务端主线程。注册在进程生命周期内持续有效（不可移除）。
 * Lookups use the raw role ({@code GameWorldComponent#getRole}), never {@code isRole}, so acting or disguise overlays
 * that widen {@code isRole} cannot change the result. Liveness is not checked here; callers keep their own alive test.
 * 查询使用原始身份（{@code GameWorldComponent#getRole}），从不使用 {@code isRole}，因此扩大 {@code isRole} 的扮演或伪装覆盖层
 * 不会改变结果。此处不判断存活；调用方保留自己的存活判断。
 *
 * <p>A role may instead be registered with a condition ({@link #registerConditionalRole}): its holders are excluded
 * only while the owner's predicate says so (SparkWitch's Charon without a running Soul Moment). The condition is part
 * of the player query {@link #isExcluded(GameWorldComponent, UUID)} only; the role-level queries
 * ({@link #containsRole}, {@link #isExcludedRole}) keep answering for unconditional roles alone, because a role by
 * itself carries no player state.
 * 身份也可以带条件注册（{@link #registerConditionalRole}）：仅当所属模组的谓词成立时其持有者才被排除（如 SparkWitch 中
 * 没有进行中亡魂时刻的卡戎）。条件只参与玩家查询 {@link #isExcluded(GameWorldComponent, UUID)}；身份级查询
 * （{@link #containsRole}、{@link #isExcludedRole}）仍只回答无条件注册的身份，因为身份本身不携带玩家状态。</p>
 */
public final class SettlementExclusions {
    private static final Set<Identifier> ROLE_IDS = ConcurrentHashMap.newKeySet();
    private static final Map<Identifier, Predicate<PlayerEntity>> CONDITIONAL_ROLES = new ConcurrentHashMap<>();
    private static final Set<Identifier> REPORTED_CONDITION_FAILURES = ConcurrentHashMap.newKeySet();

    private SettlementExclusions() {
    }

    /**
     * Thread-safe, idempotent, process-lifetime registration; null is rejected and no role lookup is required.
     * Returns true only when the identifier was not registered before.
     * 线程安全、幂等且在进程生命周期内持续有效的注册；拒绝 null，无需身份已加载。仅当该标识符此前未注册时返回 true。
     */
    public static boolean registerRole(Identifier roleId) {
        return ROLE_IDS.add(Objects.requireNonNull(roleId, "roleId"));
    }

    /**
     * Registers a role whose holders are excluded only while {@code excludedWhile} is true for that player. The
     * predicate is asked about holders of exactly this raw role and nobody else, on the thread that runs the
     * settlement query (the server thread), with the online server-side player; it must be a cheap, side-effect-free
     * read. Thread-safe and process-lifetime like {@link #registerRole}: the first condition for an identifier wins
     * and later calls return false without replacing it; nulls are rejected. A role that is also registered through
     * {@link #registerRole} stays unconditionally excluded.
     * 注册一个带条件的身份：仅当 {@code excludedWhile} 对该玩家成立时，其持有者才被排除。谓词只会被问到原始身份恰为该标识符的
     * 玩家，在执行结算查询的线程（服务端主线程）上、以在线的服务端玩家为参数调用；它必须是廉价且无副作用的读取。与
     * {@link #registerRole} 一样线程安全并在进程生命周期内有效：同一标识符以首次注册的条件为准，后续调用返回 false 且不替换；
     * 拒绝 null。同时经 {@link #registerRole} 注册的身份仍无条件排除。
     *
     * <p>Fails closed: on a client world (owner state such conditions read is usually server-only), for an offline
     * player, and when the predicate throws, the player is not excluded; a throwing predicate is logged once per role.
     * 失败即关闭：在客户端世界（此类条件读取的状态通常只存在于服务端）、玩家不在线或谓词抛出异常时，玩家不被排除；
     * 抛出异常的谓词每个身份只记录一次日志。</p>
     */
    public static boolean registerConditionalRole(Identifier roleId, Predicate<PlayerEntity> excludedWhile) {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(excludedWhile, "excludedWhile");
        return CONDITIONAL_ROLES.putIfAbsent(roleId, excludedWhile) == null;
    }

    /**
     * Unknown or null identifiers are not excluded, even if their path matches a registered id in another namespace.
     * 未注册或 null 标识符不被排除，即使其路径与其他命名空间中已注册的标识符相同。
     */
    public static boolean containsRole(@Nullable Identifier roleId) {
        return roleId != null && ROLE_IDS.contains(roleId);
    }

    /**
     * Null roles are not excluded; only the identifier matters, never the faction or allocation properties.
     * null 身份不被排除；仅检查标识符，不检查阵营或分配属性。
     */
    public static boolean isExcludedRole(@Nullable Role role) {
        return role != null && containsRole(role.identifier());
    }

    /**
     * Whether the player's raw role ({@code game.getRole(uuid)}) is excluded, unconditionally or because the role's
     * registered condition currently holds for this player; false for a null game, a null player, or a player
     * without a role. Never calls {@code isRole}. With no conditional role registered the answer is exactly
     * {@code isExcludedRole(game.getRole(uuid))}.
     * 玩家的原始身份（{@code game.getRole(uuid)}）是否被排除：无条件排除，或该身份注册的条件此刻对该玩家成立；游戏组件为
     * null、玩家为 null 或玩家没有身份时返回 false。从不调用 {@code isRole}。未注册任何带条件身份时，结果与
     * {@code isExcludedRole(game.getRole(uuid))} 完全一致。
     */
    public static boolean isExcluded(@Nullable GameWorldComponent game, @Nullable UUID player) {
        if (game == null || player == null) {
            return false;
        }
        Role role = game.getRole(player);
        if (isExcludedRole(role)) {
            return true;
        }
        if (role == null || role.identifier() == null) {
            return false;
        }
        Predicate<PlayerEntity> condition = CONDITIONAL_ROLES.get(role.identifier());
        return condition != null && holdsFor(role.identifier(), condition, serverPlayer(game, player));
    }

    /**
     * Pure guarded read of one condition: no subject (the holder is offline, or the world is a client world) and a
     * throwing predicate both answer false.
     * 对单个条件的纯受保护读取：没有判定对象（持有者不在线，或世界是客户端世界）以及谓词抛出异常时都返回 false。
     */
    static <T> boolean holdsFor(Identifier roleId, Predicate<? super T> condition, @Nullable T subject) {
        if (subject == null) {
            return false;
        }
        try {
            return condition.test(subject);
        } catch (RuntimeException | LinkageError failure) {
            if (REPORTED_CONDITION_FAILURES.add(roleId)) {
                SparkFactionApiMod.LOGGER.error(
                        "Settlement exclusion condition for {} failed; its holders are not excluded.", roleId, failure);
            }
            return false;
        }
    }

    /** The online player on a server world, else null. / 服务端世界中的在线玩家，否则为 null。 */
    @Nullable
    private static PlayerEntity serverPlayer(GameWorldComponent game, UUID player) {
        World world = ((GameWorldComponentWorldAccessor) game).sparkfactionapi$getWorld();
        return world instanceof ServerWorld ? world.getPlayerByUuid(player) : null;
    }
}
