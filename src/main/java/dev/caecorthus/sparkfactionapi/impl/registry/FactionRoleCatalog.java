package dev.caecorthus.sparkfactionapi.impl.registry;

import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns role registration and Wathe native-bucket shielding.
 * 持有角色注册与 Wathe 原生阵营桶保护。
 */
public final class FactionRoleCatalog {
    private static final Map<Role, Identifier> ROLE_FACTIONS = new LinkedHashMap<>();
    private static final Map<Role, Faction> ROLE_NATIVE_FACTIONS = new LinkedHashMap<>();

    private FactionRoleCatalog() {
    }

    public static Role registerRole(FactionRoleDefinition definition) {
        FactionRegistryBootstrap.bootstrap();
        if (!FactionCatalog.containsRegisteredFaction(definition.factionId())) {
            throw new IllegalArgumentException("Faction must be registered before roles: " + definition.factionId());
        }

        Faction nativeWatheFaction = definition.nativeWatheFaction();
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
        ROLE_NATIVE_FACTIONS.put(role, nativeWatheFaction);
        try {
            WatheRoles.registerRole(role);
        } catch (RuntimeException exception) {
            ROLE_FACTIONS.remove(role);
            ROLE_NATIVE_FACTIONS.remove(role);
            throw exception;
        }
        return role;
    }

    public static boolean isSparkFactionRole(Role role) {
        return ROLE_FACTIONS.containsKey(role);
    }

    /**
     * Makes default SparkFactionAPI roles visible to Wathe as neutral instead of NONE.
     * 让默认 SparkFactionAPI 角色在 Wathe 侧显示为中立阵营，而不是 NONE。
     */
    public static Optional<Faction> nativeFactionOverride(Role role) {
        return nativeWatheFaction(role)
                .filter(faction -> faction == Faction.NONE)
                .map(faction -> Faction.NEUTRAL);
    }

    public static Optional<Boolean> nativeNeutralOverride(Role role) {
        return nativeWatheFaction(role)
                .filter(faction -> faction == Faction.NONE)
                .map(faction -> true);
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
        Faction faction = role.getFaction();
        return switch (faction) {
            case NONE -> FactionIds.NONE;
            case CIVILIAN -> FactionIds.CIVILIAN;
            case KILLER -> FactionIds.KILLER;
            case NEUTRAL -> FactionIds.NEUTRAL;
        };
    }

    public static Collection<Role> getRolesForFaction(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        List<Role> roles = new ArrayList<>();
        for (Map.Entry<Role, Identifier> entry : ROLE_FACTIONS.entrySet()) {
            if (entry.getValue().equals(factionId)) {
                roles.add(entry.getKey());
            }
        }
        return Collections.unmodifiableList(roles);
    }

    public static void clearForTests() {
        ROLE_FACTIONS.clear();
        ROLE_NATIVE_FACTIONS.clear();
    }

    private static Optional<Faction> nativeWatheFaction(Role role) {
        return Optional.ofNullable(ROLE_NATIVE_FACTIONS.get(role));
    }
}
