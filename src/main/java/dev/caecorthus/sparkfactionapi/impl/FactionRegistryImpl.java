package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.EffectiveFactionResolver;
import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionTargetEligibility;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FactionRegistryImpl {
    private static final Map<Identifier, FactionDefinition> FACTIONS = new LinkedHashMap<>();
    private static final Map<Role, Identifier> ROLE_FACTIONS = new LinkedHashMap<>();
    private static final List<EffectiveFactionResolver> EFFECTIVE_RESOLVERS = new ArrayList<>();
    private static final List<FactionTargetEligibility> TARGET_ELIGIBILITY = new ArrayList<>();
    private static final List<FactionEconomyPolicy> ECONOMY_POLICIES = new ArrayList<>();
    private static final List<FactionInstinctPolicy> INSTINCT_POLICIES = new ArrayList<>();
    private static boolean bootstrapped;

    private FactionRegistryImpl() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        registerLegacyFaction(FactionIds.NONE, 0xFFFFFF);
        registerLegacyFaction(FactionIds.CIVILIAN, 0x36E51B);
        registerLegacyFaction(FactionIds.KILLER, 0xC13838);
        registerLegacyFaction(FactionIds.NEUTRAL, 0xB567FF);
        FactionWinService.register();
    }

    private static void registerLegacyFaction(Identifier id, int color) {
        FactionCapabilities capabilities = switch (id.getPath()) {
            case "civilian" -> FactionCapabilities.builder()
                    .isPunishableInnocentGunVictim(true)
                    .isPunishableInnocentGunShooter(true)
                    .build();
            case "killer" -> FactionCapabilities.builder()
                    .canUseKillerFeatures(true)
                    .receivesKillerPassiveMoney(true)
                    .receivesKillRewards(true)
                    .sharesCohort(true)
                    .canUseInstinct(true)
                    .instinctColor(0x990000)
                    .build();
            default -> FactionCapabilities.none();
        };
        FACTIONS.put(id, FactionDefinition.builder(id)
                .color(color)
                .translationKeyPrefix("faction." + id.getNamespace() + "." + id.getPath())
                .capabilities(capabilities)
                .build());
    }

    public static FactionDefinition registerFaction(FactionDefinition definition) {
        bootstrap();
        if (isLegacyFaction(definition.id())) {
            throw new IllegalArgumentException("Legacy wathe factions cannot be replaced: " + definition.id());
        }
        if (FACTIONS.containsKey(definition.id())) {
            throw new IllegalArgumentException("Faction already registered: " + definition.id());
        }
        FACTIONS.put(definition.id(), definition);
        return definition;
    }

    public static Role registerRole(FactionRoleDefinition definition) {
        bootstrap();
        if (!FACTIONS.containsKey(definition.factionId())) {
            throw new IllegalArgumentException("Faction must be registered before roles: " + definition.factionId());
        }

        // Custom faction roles deliberately use neutral-safe wathe booleans.
        // 自定义阵营角色底层故意使用安全的 wathe 布尔值，真实阵营由本 API 解析。
        Role role = new Role(
                definition.roleId(),
                definition.color(),
                false,
                false,
                definition.moodType(),
                definition.maxSprintTime(),
                definition.canSeeTime(),
                definition.appearanceCondition()
        );
        ROLE_FACTIONS.put(role, definition.factionId());
        try {
            WatheRoles.registerRole(role);
        } catch (RuntimeException exception) {
            ROLE_FACTIONS.remove(role);
            throw exception;
        }
        return role;
    }

    public static boolean isSparkFactionRole(Role role) {
        return ROLE_FACTIONS.containsKey(role);
    }

    /**
     * Keeps SparkFactionAPI roles out of Wathe's built-in faction buckets.
     * 防止 SparkFactionAPI 角色落入 wathe 原生阵营桶。
     */
    public static Optional<Faction> nativeFactionOverride(Role role) {
        return isSparkFactionRole(role) ? Optional.of(Faction.NONE) : Optional.empty();
    }

    public static Optional<Boolean> nativeNeutralOverride(Role role) {
        return isSparkFactionRole(role) ? Optional.of(false) : Optional.empty();
    }

    public static Identifier resolveBaseFaction(Role role) {
        bootstrap();
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

    public static Identifier resolveEffectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        bootstrap();
        if (player == null || gameComponent == null) {
            return FactionIds.NONE;
        }
        Identifier current = resolveBaseFaction(gameComponent.getRole(player));
        for (EffectiveFactionResolver resolver : EFFECTIVE_RESOLVERS) {
            Identifier resolved = resolver.resolve(player, gameComponent, current);
            if (resolved != null) {
                current = resolved;
            }
        }
        return current;
    }

    public static boolean canTarget(
            PlayerEntity viewer,
            PlayerEntity target,
            Identifier targetTag,
            GameWorldComponent gameComponent
    ) {
        bootstrap();
        return FactionCapabilityBridge.canTarget(viewer, target, targetTag, gameComponent);
    }

    public static FactionCapabilities capabilities(Identifier factionId) {
        bootstrap();
        return Optional.ofNullable(FACTIONS.get(factionId))
                .map(FactionDefinition::capabilities)
                .orElseGet(FactionCapabilities::none);
    }

    public static Optional<FactionDefinition> getFaction(Identifier factionId) {
        bootstrap();
        return Optional.ofNullable(FACTIONS.get(factionId));
    }

    public static Collection<FactionDefinition> getCustomFactions() {
        bootstrap();
        return FACTIONS.values().stream()
                .filter(faction -> !isLegacyFaction(faction.id()))
                .toList();
    }

    public static boolean isCustomFaction(Identifier factionId) {
        bootstrap();
        return FACTIONS.containsKey(factionId) && !isLegacyFaction(factionId);
    }

    public static Collection<Role> getRolesForFaction(Identifier factionId) {
        bootstrap();
        List<Role> roles = new ArrayList<>();
        for (Map.Entry<Role, Identifier> entry : ROLE_FACTIONS.entrySet()) {
            if (entry.getValue().equals(factionId)) {
                roles.add(entry.getKey());
            }
        }
        return Collections.unmodifiableList(roles);
    }

    public static List<FactionTargetEligibility> targetEligibility() {
        return TARGET_ELIGIBILITY;
    }

    public static List<FactionEconomyPolicy> economyPolicies() {
        return ECONOMY_POLICIES;
    }

    public static List<FactionInstinctPolicy> instinctPolicies() {
        return INSTINCT_POLICIES;
    }

    public static void registerEffectiveFactionResolver(EffectiveFactionResolver resolver) {
        EFFECTIVE_RESOLVERS.add(resolver);
    }

    public static void registerTargetEligibility(FactionTargetEligibility eligibility) {
        TARGET_ELIGIBILITY.add(eligibility);
    }

    public static void registerEconomyPolicy(FactionEconomyPolicy policy) {
        ECONOMY_POLICIES.add(policy);
    }

    public static void registerInstinctPolicy(FactionInstinctPolicy policy) {
        INSTINCT_POLICIES.add(policy);
    }

    static void clearForTests() {
        FACTIONS.clear();
        ROLE_FACTIONS.clear();
        EFFECTIVE_RESOLVERS.clear();
        TARGET_ELIGIBILITY.clear();
        ECONOMY_POLICIES.clear();
        INSTINCT_POLICIES.clear();
        bootstrapped = false;
    }

    private static boolean isLegacyFaction(Identifier id) {
        return FactionIds.NONE.equals(id)
                || FactionIds.CIVILIAN.equals(id)
                || FactionIds.KILLER.equals(id)
                || FactionIds.NEUTRAL.equals(id);
    }
}
