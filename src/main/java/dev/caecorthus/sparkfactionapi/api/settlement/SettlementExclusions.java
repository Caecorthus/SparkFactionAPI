package dev.caecorthus.sparkfactionapi.api.settlement;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 */
public final class SettlementExclusions {
    private static final Set<Identifier> ROLE_IDS = ConcurrentHashMap.newKeySet();

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
     * Whether the player's raw role ({@code game.getRole(uuid)}) is excluded; false for a null game, a null player,
     * or a player without a role. Never calls {@code isRole}.
     * 玩家的原始身份（{@code game.getRole(uuid)}）是否被排除；游戏组件为 null、玩家为 null 或玩家没有身份时返回 false。
     * 从不调用 {@code isRole}。
     */
    public static boolean isExcluded(@Nullable GameWorldComponent game, @Nullable UUID player) {
        return game != null && player != null && isExcludedRole(game.getRole(player));
    }

    /**
     * Convenience overload reading the {@link GameWorldComponent} of the player's world; false for a null player, a
     * missing world, or a world without the component.
     * 读取玩家所在世界的 {@link GameWorldComponent} 的便捷重载；玩家为 null、没有世界或世界上没有该组件时返回 false。
     */
    public static boolean isExcluded(@Nullable PlayerEntity player) {
        if (player == null) {
            return false;
        }
        World world = player.getWorld();
        return world != null && isExcluded(GameWorldComponent.KEY.getNullable(world), player.getUuid());
    }

    /**
     * Immutable snapshot of the registered identifiers; later registrations do not change a returned set.
     * 已注册标识符的不可变快照；之后的注册不会改变已返回的集合。
     */
    public static Set<Identifier> roleIds() {
        return Set.copyOf(ROLE_IDS);
    }
}
