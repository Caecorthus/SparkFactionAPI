package dev.caecorthus.sparkfactionapi.impl.registry;

import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeWatheFactions;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns role registration, role-to-faction bindings and Wathe native-bucket shielding.
 * 持有角色注册、角色阵营绑定与 Wathe 原生阵营桶保护。
 */
public final class FactionRoleCatalog {
    private static final Map<Role, Identifier> ROLE_FACTIONS = new LinkedHashMap<>();
    // What Faction#fromRole returns for an SFA role: NEUTRAL (NONE shielding) or a native custom constant.
    // SFA 角色在 Faction#fromRole 中的返回值：NEUTRAL（NONE 保护）或原生自定义常量。
    private static final Map<Role, Faction> ROLE_WATHE_OVERRIDES = new HashMap<>();
    // Copy-on-write snapshots so game threads read bindings without locks. 写时复制快照，游戏线程无锁读取。
    private static volatile Map<Identifier, Identifier> boundRoleFactions = Map.of();
    private static volatile Map<Identifier, Faction> boundRoleConstants = Map.of();

    private FactionRoleCatalog() {
    }

    public static Role registerRole(FactionRoleDefinition definition) {
        FactionRegistryBootstrap.bootstrap();
        if (!FactionCatalog.containsRegisteredFaction(definition.factionId())) {
            throw new IllegalArgumentException("Faction must be registered before roles: " + definition.factionId());
        }
        if (boundRoleFactions.containsKey(definition.roleId())) {
            throw new IllegalArgumentException("Role " + definition.roleId() + " is bound to "
                    + boundRoleFactions.get(definition.roleId()) + " with bindRoleFaction and cannot also be registered");
        }

        Faction nativeWatheFaction = definition.nativeWatheFaction();
        Faction wathe = watheFactionFor(definition, nativeWatheFaction);
        // Custom factions keep their real Identifier in SparkFactionAPI while Wathe sees their native bucket.
        // 自定义阵营的真实 Identifier 由 SparkFactionAPI 保存，Wathe 只读取它们的原生阵营桶。
        Role role = new Role(
                definition.roleId(),
                definition.color(),
                nativeWatheFaction == Faction.CIVILIAN,
                nativeWatheFaction == Faction.KILLER,
                definition.moodType(),
                definition.maxSprintTime(),
                definition.canSeeTime(),
                definition.appearanceCondition()
        );
        ROLE_FACTIONS.put(role, definition.factionId());
        if (wathe != null) {
            ROLE_WATHE_OVERRIDES.put(role, wathe);
        }
        try {
            WatheRoles.registerRole(role);
        } catch (RuntimeException exception) {
            ROLE_FACTIONS.remove(role);
            ROLE_WATHE_OVERRIDES.remove(role);
            throw exception;
        }
        return role;
    }

    /**
     * Wathe faction override of a newly registered role, or null when Wathe's own fromRole already answers (an
     * explicit CIVILIAN, KILLER or NEUTRAL bucket). A NONE bucket means "this faction": its native constant when the
     * faction is declared native, NEUTRAL otherwise (the pre-0.1.6.0 shielding). An explicit appended constant must
     * be the role faction's own constant.
     * 新注册角色的 Wathe 阵营覆盖；显式 CIVILIAN、KILLER 或 NEUTRAL 原生桶由 Wathe 自身 fromRole 回答，返回 null。
     * NONE 表示“本阵营”：阵营声明为原生时用其原生常量，否则为 NEUTRAL（0.1.6.0 之前的保护）。显式指定追加常量时
     * 必须是该角色阵营自己的常量。
     */
    private static @Nullable Faction watheFactionFor(FactionRoleDefinition definition, Faction nativeWatheFaction) {
        Faction factionConstant = NativeWatheFactions.customConstantOrNull(definition.factionId());
        if (nativeWatheFaction == Faction.NONE) {
            return factionConstant != null ? factionConstant : Faction.NEUTRAL;
        }
        if (NativeWatheFactions.customFactionIdOrNull(nativeWatheFaction) != null) {
            if (nativeWatheFaction != factionConstant) {
                throw new IllegalArgumentException("Role " + definition.roleId() + " uses native Wathe faction "
                        + nativeWatheFaction.name() + ", which belongs to "
                        + NativeWatheFactions.customFactionIdOrNull(nativeWatheFaction) + ", not " + definition.factionId());
            }
            return nativeWatheFaction;
        }
        return null;
    }

    /**
     * Binds a role SparkFactionAPI did not register (for example a NoellesRoles role) to a registered custom faction.
     * Resolved lazily by role id, so the role may be registered with Wathe before or after this call.
     * 把非 SparkFactionAPI 注册的角色（例如 NoellesRoles 角色）绑定到已注册的自定义阵营。按角色 id 惰性解析，
     * 因此该角色在 Wathe 中注册早于或晚于本调用都可以。
     */
    public static synchronized void bindRoleFaction(Identifier roleId, Identifier factionId) {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(factionId, "factionId");
        FactionRegistryBootstrap.bootstrap();
        if (!FactionCatalog.isCustomFaction(factionId)) {
            throw new IllegalArgumentException("Roles can only be bound to a registered custom faction: " + factionId);
        }
        for (Role role : ROLE_FACTIONS.keySet()) {
            if (role.identifier().equals(roleId)) {
                throw new IllegalArgumentException("Role " + roleId + " is registered through SparkFactionAPI; "
                        + "its faction comes from its FactionRoleDefinition");
            }
        }
        Identifier existing = boundRoleFactions.get(roleId);
        if (existing != null) {
            if (existing.equals(factionId)) {
                return;
            }
            throw new IllegalStateException("Role " + roleId + " is already bound to " + existing + ", not " + factionId);
        }
        Map<Identifier, Identifier> factions = new LinkedHashMap<>(boundRoleFactions);
        factions.put(roleId, factionId);
        boundRoleFactions = Collections.unmodifiableMap(factions);
        Faction constant = NativeWatheFactions.customConstantOrNull(factionId);
        if (constant != null) {
            Map<Identifier, Faction> constants = new LinkedHashMap<>(boundRoleConstants);
            constants.put(roleId, constant);
            boundRoleConstants = Collections.unmodifiableMap(constants);
        }
    }

    /**
     * Makes default SparkFactionAPI roles visible to Wathe as neutral instead of NONE, roles of a native faction (and
     * roles bound to one) as its constant.
     * 让默认 SparkFactionAPI 角色在 Wathe 侧显示为中立阵营而不是 NONE，原生阵营的角色（及绑定到原生阵营的角色）
     * 显示为其常量。
     */
    public static Optional<Faction> nativeFactionOverride(Role role) {
        return Optional.ofNullable(nativeFactionOverrideOrNull(role));
    }

    /**
     * Allocation-free form of {@link #nativeFactionOverride} for the hot Faction#fromRole mixin; null means no override.
     * 供高频 Faction#fromRole mixin 使用的无分配版本 {@link #nativeFactionOverride}；null 表示不覆盖。
     */
    public static @Nullable Faction nativeFactionOverrideOrNull(@Nullable Role role) {
        if (role == null) {
            return null;
        }
        Faction override = ROLE_WATHE_OVERRIDES.get(role);
        if (override != null) {
            return override;
        }
        Map<Identifier, Faction> bound = boundRoleConstants;
        if (bound.isEmpty()) {
            return null;
        }
        Identifier roleId = role.identifier();
        return roleId == null ? null : bound.get(roleId);
    }

    public static Optional<Boolean> nativeNeutralOverride(Role role) {
        return Optional.ofNullable(nativeNeutralOverrideOrNull(role));
    }

    /**
     * Allocation-free form of {@link #nativeNeutralOverride} for the hot Role#isNeutral mixin; null means no override.
     * NONE-shielded roles are neutral; roles shown as a native custom constant are not.
     * 供高频 Role#isNeutral mixin 使用的无分配版本 {@link #nativeNeutralOverride}；null 表示不覆盖。
     * 受 NONE 保护的角色为中立；显示为原生自定义常量的角色不是中立。
     */
    public static @Nullable Boolean nativeNeutralOverrideOrNull(@Nullable Role role) {
        Faction override = nativeFactionOverrideOrNull(role);
        if (override == null) {
            return null;
        }
        return override == Faction.NEUTRAL ? Boolean.TRUE : Boolean.FALSE;
    }

    public static Identifier resolveBaseFaction(Role role) {
        FactionRegistryBootstrap.bootstrap();
        if (role == null || role == WatheRoles.NO_ROLE) {
            return FactionIds.NONE;
        }
        Identifier customFaction = ROLE_FACTIONS.get(role);
        if (customFaction != null) {
            return customFaction;
        }
        Map<Identifier, Identifier> bound = boundRoleFactions;
        if (!bound.isEmpty() && role.identifier() != null) {
            Identifier boundFaction = bound.get(role.identifier());
            if (boundFaction != null) {
                return boundFaction;
            }
        }
        // Reverse lookup instead of an exhaustive switch: appended constants must never throw MatchException.
        // An undeclared constant (appended by some other mod) reads as neutral, Wathe's "neither side" bucket.
        // 使用反查而非穷举 switch：追加的常量绝不能抛出 MatchException。未声明的常量（其它模组追加）视为中立。
        Identifier factionId = NativeWatheFactions.factionIdOrNull(role.getFaction());
        return factionId != null ? factionId : FactionIds.NEUTRAL;
    }

    /**
     * Registered roles of the faction in registration order, then bound roles that Wathe currently knows.
     * 该阵营的注册角色（按注册顺序），随后是 Wathe 当前已知的绑定角色。
     */
    public static Collection<Role> getRolesForFaction(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        List<Role> roles = new ArrayList<>();
        for (Map.Entry<Role, Identifier> entry : ROLE_FACTIONS.entrySet()) {
            if (entry.getValue().equals(factionId)) {
                roles.add(entry.getKey());
            }
        }
        for (Map.Entry<Identifier, Identifier> binding : boundRoleFactions.entrySet()) {
            if (binding.getValue().equals(factionId)) {
                Role role = WatheRoles.getRole(binding.getKey());
                if (role != null && !roles.contains(role)) {
                    roles.add(role);
                }
            }
        }
        return Collections.unmodifiableList(roles);
    }
}
