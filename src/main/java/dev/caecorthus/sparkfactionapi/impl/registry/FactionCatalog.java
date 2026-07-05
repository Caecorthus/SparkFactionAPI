package dev.caecorthus.sparkfactionapi.impl.registry;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Owns faction definitions and capability lookup.
 * 持有阵营定义与能力查询。
 */
public final class FactionCatalog {
    private static final Map<Identifier, FactionDefinition> FACTIONS = new LinkedHashMap<>();

    private FactionCatalog() {
    }

    static void registerLegacyFaction(Identifier id, int color) {
        FactionCapabilities capabilities = switch (id.getPath()) {
            case "civilian" -> FactionCapabilities.builder()
                    .isPunishableInnocentGunVictim(true)
                    .isPunishableInnocentGunShooter(true)
                    .build();
            case "killer" -> FactionCapabilities.builder()
                    .canUseKillerFeatures(true)
                    .receivesKillerPassiveMoney(true)
                    .receivesKillRewards(true)
                    .hasBlackoutImmunity(true)
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
        FactionRegistryBootstrap.bootstrap();
        if (isLegacyFaction(definition.id())) {
            throw new IllegalArgumentException("Legacy wathe factions cannot be replaced: " + definition.id());
        }
        if (FACTIONS.containsKey(definition.id())) {
            throw new IllegalArgumentException("Faction already registered: " + definition.id());
        }
        FACTIONS.put(definition.id(), definition);
        return definition;
    }

    public static FactionCapabilities capabilities(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        return Optional.ofNullable(FACTIONS.get(factionId))
                .map(FactionDefinition::capabilities)
                .orElseGet(FactionCapabilities::none);
    }

    public static Optional<FactionDefinition> getFaction(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        return Optional.ofNullable(FACTIONS.get(factionId));
    }

    public static Collection<FactionDefinition> getCustomFactions() {
        FactionRegistryBootstrap.bootstrap();
        return FACTIONS.values().stream()
                .filter(faction -> !isLegacyFaction(faction.id()))
                .toList();
    }

    public static boolean isCustomFaction(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        return FACTIONS.containsKey(factionId) && !isLegacyFaction(factionId);
    }

    static boolean containsRegisteredFaction(Identifier factionId) {
        return FACTIONS.containsKey(factionId);
    }

    public static void clearForTests() {
        FACTIONS.clear();
    }

    private static boolean isLegacyFaction(Identifier id) {
        return FactionIds.NONE.equals(id)
                || FactionIds.CIVILIAN.equals(id)
                || FactionIds.KILLER.equals(id)
                || FactionIds.NEUTRAL.equals(id);
    }
}
