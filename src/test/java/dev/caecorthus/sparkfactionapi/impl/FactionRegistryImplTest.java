package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.registry.EffectiveFactionResolvers;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionRegistryImplTest {
    @BeforeEach
    void setUp() {
        FactionRegistryImpl.clearForTests();
        SparkFactionApi.bootstrap();
    }

    @AfterEach
    void tearDown() {
        FactionRegistryImpl.clearForTests();
    }

    @Test
    void legacyWatheRolesKeepTheirFallbackFactions() {
        assertEquals(FactionIds.NONE, SparkFactionApi.resolveBaseFaction(WatheRoles.NO_ROLE));
        assertEquals(FactionIds.CIVILIAN, SparkFactionApi.resolveBaseFaction(WatheRoles.CIVILIAN));
        assertEquals(FactionIds.KILLER, SparkFactionApi.resolveBaseFaction(WatheRoles.KILLER));
        assertEquals(FactionIds.NEUTRAL, SparkFactionApi.resolveBaseFaction(WatheRoles.LOOSE_END));
    }

    @Test
    void customRolesResolveToTheirRegisteredFaction() {
        Identifier witchFaction = Identifier.of("sparkwitch", "witch");
        SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction)
                .color(0x7D2AFF)
                .build());

        Role highWitch = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "high_witch"),
                        witchFaction
                )
                .color(0x7D2AFF)
                .build());

        assertEquals(witchFaction, SparkFactionApi.resolveBaseFaction(highWitch));
        assertFalse(highWitch.isInnocent());
        assertFalse(highWitch.canUseKiller());
    }

    @Test
    void customRolesExposeNeutralNativeWatheFaction() {
        Identifier witchFaction = Identifier.of("sparkwitch", "native_neutral");
        SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction).build());

        Role witch = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "native_neutral_witch"),
                        witchFaction
                )
                .build());

        assertEquals(witchFaction, SparkFactionApi.resolveBaseFaction(witch));
        assertEquals(Faction.NEUTRAL, FactionRegistryImpl.nativeFactionOverride(witch).orElseThrow());
        assertTrue(FactionRegistryImpl.nativeNeutralOverride(witch).orElseThrow());
        assertTrue(FactionRegistryImpl.nativeFactionOverride(WatheRoles.LOOSE_END).isEmpty());
    }

    @Test
    void customRolesParticipateInSimulatedNativeNeutralRolePool() {
        Identifier witchFaction = Identifier.of("sparkwitch", "pool_visible");
        SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction).build());

        Role witch = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "pool_visible_witch"),
                        witchFaction
                )
                .build());
        List<Role> roles = List.of(WatheRoles.CIVILIAN, WatheRoles.KILLER, WatheRoles.LOOSE_END, witch);

        assertFalse(containsNativePoolRole(roles, witch, Faction.CIVILIAN));
        assertFalse(containsNativePoolRole(roles, witch, Faction.KILLER));
        assertTrue(containsNativePoolRole(roles, witch, Faction.NEUTRAL));
    }

    @Test
    void customRolesCanOptIntoNativeWatheCivilianPool() {
        Identifier factionId = FactionIds.CIVILIAN;

        Role passengerSpecialist = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "passenger_specialist"),
                        factionId
                )
                .nativeWatheFaction(Faction.CIVILIAN)
                .build());
        List<Role> roles = List.of(WatheRoles.CIVILIAN, passengerSpecialist);

        assertEquals(factionId, SparkFactionApi.resolveBaseFaction(passengerSpecialist));
        assertTrue(passengerSpecialist.isInnocent());
        assertFalse(passengerSpecialist.canUseKiller());
        assertTrue(FactionRegistryImpl.nativeFactionOverride(passengerSpecialist).isEmpty());
        assertTrue(FactionRegistryImpl.nativeNeutralOverride(passengerSpecialist).isEmpty());
        assertTrue(containsNativePoolRole(roles, passengerSpecialist, Faction.CIVILIAN));
    }

    @Test
    void customRolesCanOptIntoNativeWatheKillerPool() {
        Identifier factionId = FactionIds.KILLER;

        Role covenHunter = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "coven_hunter"),
                        factionId
                )
                .nativeWatheFaction(Faction.KILLER)
                .build());
        List<Role> roles = List.of(WatheRoles.KILLER, covenHunter);

        assertEquals(factionId, SparkFactionApi.resolveBaseFaction(covenHunter));
        assertFalse(covenHunter.isInnocent());
        assertTrue(covenHunter.canUseKiller());
        assertTrue(FactionRegistryImpl.nativeFactionOverride(covenHunter).isEmpty());
        assertTrue(FactionRegistryImpl.nativeNeutralOverride(covenHunter).isEmpty());
        assertTrue(containsNativePoolRole(roles, covenHunter, Faction.KILLER));
    }

    @Test
    void duplicateAndLegacyFactionRegistrationAreRejected() {
        Identifier witchFaction = Identifier.of("sparkwitch", "dupe_witch");
        SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction).build());

        assertThrows(IllegalArgumentException.class,
                () -> SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction).build()));
        assertThrows(IllegalArgumentException.class,
                () -> SparkFactionApi.registerFaction(FactionDefinition.builder(FactionIds.KILLER).build()));
    }

    @Test
    void roleRegistrationRequiresExistingFaction() {
        Identifier missingFaction = Identifier.of("sparkwitch", "missing_faction");

        assertThrows(IllegalArgumentException.class, () -> SparkFactionApi.registerRole(
                FactionRoleDefinition.builder(Identifier.of("sparkwitch", "missing_role"), missingFaction).build()
        ));
    }

    @Test
    void customFactionCollectionKeepsRegistrationOrderAndExcludesLegacy() {
        Identifier firstFaction = Identifier.of("sparkwitch", "first_custom");
        Identifier secondFaction = Identifier.of("sparkwitch", "second_custom");
        SparkFactionApi.registerFaction(FactionDefinition.builder(firstFaction).build());
        SparkFactionApi.registerFaction(FactionDefinition.builder(secondFaction).build());

        List<Identifier> factionIds = SparkFactionApi.getCustomFactions().stream()
                .map(FactionDefinition::id)
                .toList();

        assertEquals(List.of(firstFaction, secondFaction), factionIds);
    }

    @Test
    void rolesForFactionKeepRegistrationOrderAndAreReadOnly() {
        Identifier factionId = Identifier.of("sparkwitch", "ordered_roles");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role firstRole = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "ordered_first"),
                        factionId
                )
                .build());
        Role secondRole = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "ordered_second"),
                        factionId
                )
                .build());

        List<Role> roles = List.copyOf(SparkFactionApi.getRolesForFaction(factionId));

        assertEquals(List.of(firstRole, secondRole), roles);
        assertThrows(UnsupportedOperationException.class,
                () -> SparkFactionApi.getRolesForFaction(factionId).clear());
    }

    @Test
    void effectiveFactionResolversApplyInRegistrationOrderAndIgnoreNulls() {
        Identifier baseFaction = Identifier.of("sparkwitch", "base_witch");
        Identifier middleFaction = Identifier.of("sparkwitch", "middle_witch");
        Identifier finalFaction = Identifier.of("sparkwitch", "final_witch");
        SparkFactionApi.registerEffectiveFactionResolver((player, gameComponent, currentFaction) -> null);
        SparkFactionApi.registerEffectiveFactionResolver((player, gameComponent, currentFaction) -> middleFaction);
        SparkFactionApi.registerEffectiveFactionResolver((player, gameComponent, currentFaction) ->
                middleFaction.equals(currentFaction) ? finalFaction : FactionIds.NONE);

        assertEquals(finalFaction, EffectiveFactionResolvers.resolveFrom(baseFaction, null, null));
    }

    @Test
    void registryTestResetClearsEffectiveFactionResolvers() {
        Identifier baseFaction = Identifier.of("sparkwitch", "resolver_base");
        Identifier overrideFaction = Identifier.of("sparkwitch", "resolver_override");
        SparkFactionApi.registerEffectiveFactionResolver((player, gameComponent, currentFaction) -> overrideFaction);

        assertEquals(overrideFaction, EffectiveFactionResolvers.resolveFrom(baseFaction, null, null));

        FactionRegistryImpl.clearForTests();
        SparkFactionApi.bootstrap();

        assertEquals(baseFaction, EffectiveFactionResolvers.resolveFrom(baseFaction, null, null));
    }

    @Test
    void customFactionCapabilitiesDefaultOff() {
        Identifier witchFaction = Identifier.of("sparkwitch", "silent_witch");
        SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction).build());

        FactionCapabilities capabilities = SparkFactionApi.capabilities(witchFaction);

        assertFalse(capabilities.canUseKillerFeatures());
        assertFalse(capabilities.receivesKillerPassiveMoney());
        assertFalse(capabilities.receivesKillRewards());
        assertFalse(capabilities.isPunishableInnocentGunVictim());
        assertFalse(capabilities.isPunishableInnocentGunShooter());
        assertFalse(capabilities.hasBlackoutImmunity());
        assertFalse(capabilities.sharesCohort());
        assertFalse(capabilities.canUseInstinct());
        assertTrue(capabilities.targetTags().isEmpty());
    }

    @Test
    void blackoutImmunityCapabilitySurvivesBuilderCopy() {
        FactionCapabilities capabilities = FactionCapabilities.builder()
                .hasBlackoutImmunity(true)
                .build();

        assertTrue(capabilities.toBuilder().build().hasBlackoutImmunity());
    }

    @Test
    void defaultTranslationPrefixUsesResourceKeySafeIdentifierPath() {
        FactionDefinition faction = FactionDefinition.builder(Identifier.of("sparkwitch", "coven/witches")).build();

        assertEquals("faction.sparkwitch.coven.witches", faction.translationKeyPrefix());
    }

    private static boolean containsNativePoolRole(List<Role> roles, Role role, Faction faction) {
        return roles.stream()
                .filter(candidate -> FactionRegistryImpl.nativeFactionOverride(candidate)
                        .orElseGet(candidate::getFaction) == faction)
                .anyMatch(candidate -> candidate == role);
    }
}
