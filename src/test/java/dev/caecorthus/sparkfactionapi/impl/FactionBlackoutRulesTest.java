package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.blackout.FactionBlackoutRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionBlackoutRulesTest {
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
    void customFactionRolesDefaultToNoBlackoutImmunity() {
        Identifier factionId = Identifier.of("sparkwitch", "blackout_safe_default");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "blackout_safe_default_witch"),
                        factionId
                )
                .build());

        assertFalse(FactionBlackoutRules.hasBlackoutImmunity(role));
        assertFalse(SparkFactionApi.hasBlackoutImmunity(role));
    }

    @Test
    void legacyKillerBlackoutImmunityStaysCompatible() {
        assertTrue(FactionBlackoutRules.hasBlackoutImmunity(WatheRoles.KILLER));
        assertTrue(SparkFactionApi.hasBlackoutImmunity(WatheRoles.KILLER));
    }

    @Test
    void customFactionCanGrantBlackoutImmunityWithoutKillerFeatures() {
        Identifier factionId = Identifier.of("sparkwitch", "blackout_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .capabilities(FactionCapabilities.builder()
                        .hasBlackoutImmunity(true)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "blackout_safe_witch"),
                        factionId
                )
                .build());

        assertTrue(FactionBlackoutRules.hasBlackoutImmunity(role));
        assertTrue(SparkFactionApi.hasBlackoutImmunity(role));
        assertFalse(SparkFactionApi.capabilities(factionId).canUseKillerFeatures());
    }

    @Test
    void killerFeatureAccessStillGrantsBlackoutImmunityForCompatibility() {
        Identifier factionId = Identifier.of("sparkwitch", "legacy_shadow");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .capabilities(FactionCapabilities.builder()
                        .canUseKillerFeatures(true)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "legacy_shadow_witch"),
                        factionId
                )
                .build());

        assertTrue(FactionBlackoutRules.hasBlackoutImmunity(role));
        assertTrue(SparkFactionApi.hasBlackoutImmunity(role));
    }

    @Test
    void playerBlackoutImmunityQueryDefaultsFalseWithoutContext() {
        assertFalse(FactionBlackoutRules.hasBlackoutImmunity(null, null));
        assertFalse(SparkFactionApi.hasBlackoutImmunity(null, null));
        assertFalse(FactionBlackoutRules.sharesCustomBlackoutCooldown(null, null));
    }

    @Test
    void blackoutCooldownPolicyUsesFirstNonNullOverride() {
        SparkFactionApi.registerBlackoutCooldownPolicy((purchaser, target, gameComponent) -> null);
        SparkFactionApi.registerBlackoutCooldownPolicy((purchaser, target, gameComponent) -> Boolean.TRUE);
        SparkFactionApi.registerBlackoutCooldownPolicy((purchaser, target, gameComponent) -> Boolean.FALSE);

        assertEquals(Boolean.TRUE, FactionBlackoutRules.blackoutCooldownOverride(null, null, null));
    }

    @Test
    void blackoutCooldownPolicyCanReturnFalseOverride() {
        SparkFactionApi.registerBlackoutCooldownPolicy((purchaser, target, gameComponent) -> Boolean.FALSE);

        assertEquals(Boolean.FALSE, FactionBlackoutRules.blackoutCooldownOverride(null, null, null));
    }

    @Test
    void registryTestResetClearsBlackoutCooldownPolicies() {
        SparkFactionApi.registerBlackoutCooldownPolicy((purchaser, target, gameComponent) -> Boolean.TRUE);

        assertEquals(Boolean.TRUE, FactionBlackoutRules.blackoutCooldownOverride(null, null, null));

        FactionRegistryImpl.clearForTests();

        assertNull(FactionBlackoutRules.blackoutCooldownOverride(null, null, null));
    }
}
