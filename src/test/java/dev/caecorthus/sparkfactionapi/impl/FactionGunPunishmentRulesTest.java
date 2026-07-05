package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.gun.FactionGunPunishmentRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionGunPunishmentRulesTest {
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
    void customFactionRolesDefaultToNoGunPunishment() {
        Identifier factionId = Identifier.of("sparkwitch", "gun_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "gun_safe_witch"),
                        factionId
                )
                .build());

        assertFalse(FactionGunPunishmentRules.isPunishableInnocentGunVictim(role));
        assertFalse(FactionGunPunishmentRules.isPunishableInnocentGunShooter(role));
        assertFalse(FactionGunPunishmentRules.consumesPunishableGunLikeKiller(role));
    }

    @Test
    void legacyCivilianFallbackCapabilitiesStayCompatible() {
        assertTrue(FactionGunPunishmentRules.isPunishableInnocentGunVictim(WatheRoles.CIVILIAN));
        assertTrue(FactionGunPunishmentRules.isPunishableInnocentGunShooter(WatheRoles.CIVILIAN));
        assertFalse(FactionGunPunishmentRules.consumesPunishableGunLikeKiller(WatheRoles.CIVILIAN));
    }

    @Test
    void customPunishableShooterConsumesGunLikeKillerWithoutKillerFeatureAccess() {
        Identifier factionId = Identifier.of("sparkwitch", "gun_spend_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .capabilities(FactionCapabilities.builder()
                        .isPunishableInnocentGunShooter(true)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "gun_spend_witch"),
                        factionId
                )
                .build());

        assertTrue(FactionGunPunishmentRules.isPunishableInnocentGunShooter(role));
        assertTrue(FactionGunPunishmentRules.consumesPunishableGunLikeKiller(role));
    }

    @Test
    void gunPunishmentPolicyUsesFirstNonNullOverride() {
        SparkFactionApi.registerGunPunishmentPolicy((player, subject, gameComponent) -> null);
        SparkFactionApi.registerGunPunishmentPolicy((player, subject, gameComponent) ->
                subject == FactionGunPunishmentPolicy.Subject.VICTIM ? Boolean.TRUE : Boolean.FALSE);
        SparkFactionApi.registerGunPunishmentPolicy((player, subject, gameComponent) ->
                subject == FactionGunPunishmentPolicy.Subject.VICTIM ? Boolean.FALSE : Boolean.TRUE);

        assertTrue(FactionGunPunishmentRules.isPunishableInnocentGunVictim(null, null));
        assertFalse(FactionGunPunishmentRules.isPunishableInnocentGunShooter(null, null));
    }

    @Test
    void registryTestResetClearsGunPunishmentPolicies() {
        SparkFactionApi.registerGunPunishmentPolicy((player, subject, gameComponent) -> Boolean.TRUE);

        assertTrue(FactionGunPunishmentRules.isPunishableInnocentGunVictim(null, null));

        FactionRegistryImpl.clearForTests();

        assertFalse(FactionGunPunishmentRules.isPunishableInnocentGunVictim(null, null));
    }
}
