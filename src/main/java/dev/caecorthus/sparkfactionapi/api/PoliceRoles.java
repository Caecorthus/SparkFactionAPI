package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Identifier-based police classification, independent of factions, capabilities, and allocation.
 * 基于身份标识符的警职分类，与阵营、能力及身份分配无关。
 * Only native vigilante/veteran are defaults; other role owners opt in during mod initialization.
 * 仅原生义警和退伍军人为默认成员；其他身份由所属模组在初始化时主动注册。
 */
public final class PoliceRoles {
    private static final Set<Identifier> ROLE_IDS = ConcurrentHashMap.newKeySet();

    static {
        ROLE_IDS.add(Identifier.of("wathe", "vigilante"));
        ROLE_IDS.add(Identifier.of("wathe", "veteran"));
    }

    private PoliceRoles() {
    }

    /**
     * Thread-safe, idempotent, process-lifetime registration; null is rejected and no role lookup is required.
     * 线程安全、幂等且在进程生命周期内持续有效的注册；拒绝 null，无需身份已加载。
     */
    public static void register(Identifier roleId) {
        ROLE_IDS.add(Objects.requireNonNull(roleId, "roleId"));
    }

    /**
     * Null roles or identifiers are not police; only the identifier matters, never the faction.
     * null 身份或标识符不属于警职；仅检查标识符，不检查阵营。
     */
    public static boolean contains(Role role) {
        return role != null && contains(role.identifier());
    }

    /**
     * Unknown or null identifiers are not police, even if their path matches another namespace.
     * 未注册或 null 标识符不属于警职，即使其路径与其他命名空间的警职相同。
     */
    public static boolean contains(Identifier roleId) {
        return roleId != null && ROLE_IDS.contains(roleId);
    }
}
